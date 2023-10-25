package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class manages the dealer's threads and data
 *
 * @inv 0 <= order.get(i) < config.tableSize (for 0<=i<config.tableSize)
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     * If config.turnTimeoutMillis == 0, hold the time of the last action (starting time)
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * list of slots in the table grid.
     */
    List<Integer> order;

    /**
     * True iff timer should be at warning state.
     */
    private boolean warn;

    /**
     * True iff the deck is empty and there are no sets on the table
     */
    private boolean noSets;

    /**
     * The next time the dealer should update timer display
     */
    private long sleepingTill = Long.MAX_VALUE;

    /**
     * False iff the game stop without finish
     */
    private boolean finishGame;

    /**
     * The thread representing the dealer.
     */
    private Thread dealerThread;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param table  - the table object.
     * @param players - list of players in the game.
     */
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        order = new ArrayList<>();
        for (int i = 0; i < env.config.tableSize; i++) {
            order.add(i);
        }
        warn = false;
        noSets = false;
        finishGame = true;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        dealerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        // start players' threads
        for (Player p : players) {
            Thread T = new Thread(p);
            T.start();
        }
        // main loop
        shuffelCards();
        updateTimerDisplay(true);
        while (!shouldFinish()) {
            placeCardsOnTable();
            table.freezeGame = false;
            synchronized (this) {
                this.notifyAll();
            }
            updateTimerDisplay(true); // start timer only after the placing cards
            timerLoop();
            updateTimerDisplay(true);
            removeAllCardsFromTable();
        }
        if (finishGame) {
            announceWinners();
            terminate();
            if (env.config.endGamePauseMillies > 0) {
                try {
                    Thread.sleep(env.config.endGamePauseMillies);
                } catch (InterruptedException ignored) {}
            }
        }
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && ((env.config.turnTimeoutMillis > 0 && System.currentTimeMillis() < reshuffleTime) || (env.config.turnTimeoutMillis <= 0))) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
            if (noSets) 
                break;
        }
        noSets = false;
    }

    /**
     * Called when the game should be terminated.
     * 
     * @pre  - terminate == false
     * @post - all Player Threads terminated
     * @post - terminate == true
     */
    public void terminate() {
        try {
            for (int i = players.length - 1; i >= 0; i--) {
                players[i].terminate();
                players[i].getPlayerThread().interrupt();
                players[i].getPlayerThread().join();
            }
            finishGame = false;
            terminate = true;
            dealerThread.interrupt();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        synchronized (table.setsDeclared) {
            while (!table.setsDeclared.isEmpty()) {
                int playerId = table.setsDeclared.remove();
                List<Integer> tokenToCheck = table.tokens[playerId];
                int flag = Table.FLAG_CANCELLED_SET;
                if (tokenToCheck.size() == Table.SET_SIZE) { // need to test set
                    flag = Table.FLAG_ILLEGAL_SET;
                    int[] cards = slotsToCards(tokenToCheck);
                    boolean legal = env.util.testSet(cards);
                    if (legal) { // legal set
                        flag = Table.FLAG_LEGAL_SET;
                        // remove cards and tokens
                        int[] slotsToRemove = new int[Table.SET_SIZE];
                        for (int i = 0; i < slotsToRemove.length; i++)
                            slotsToRemove[i] = tokenToCheck.get(i);
                        synchronized (table) {
                            for (int i = 0; i < slotsToRemove.length; i++) {
                                removeCardFromTable(slotsToRemove[i]);
                            }
                        }
                        updateTimerDisplay(true);
                    }
                }
                synchronized (players[playerId]) { // send feedback to player about the set
                    players[playerId].setChecked(flag);
                    players[playerId].notifyAll();
                }
            }
        }
    }

    /**
     * remove one card and all the tokens on it from the table
     * 
     * @param slot the slot to remove from it
     * 
     * @pre  - table.slotToCard[@param slot ] != null
     * @post - table.slotToCard[@param slot ] == null
     */
    private void removeCardFromTable(int slot) {
        table.removeTokens(slot);
        table.removeCard(slot); // remove card
    }

    /**
     * Mapping between slots and the cards placed in them.
     * 
     * @param slots array of slots to read cards placed in them
     * 
     * @return array of cards which placed in @param slots 
     * 
     * @pre  - 0 <= @param slots [i] <= config.tableSize (for 0<=i<@param slots .length)
     * @post - the cards returned are excatly the cards placed in @param slots
     */
    private int[] slotsToCards(List<Integer> slots) {
        int[] cards = new int[slots.size()];
        int index = 0;
        for (int slot : slots){
            cards[index] = table.slotToCard[slot];
            index++;
        }
        return cards;
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     * 
     * @post - deck.isEmpty() || table.countCards() == config.tableSize
     */
    void placeCardsOnTable() {
        Collections.shuffle(order); // random order
        synchronized (table) { // place cards
            boolean changed = false;
            for (int i : order) {
                if (!deck.isEmpty() && table.slotToCard[i] == null) {
                    int card = deck.remove(0);
                    table.placeCard(card, i);
                    changed = true;
                }
            }
            if (env.config.hints && changed) // show hints
                table.hints();
            // if the deck is empty and there is no set on the table - end the game
            // if env.config.turnTimeoutMillis <= 0, must check there is at least one legal set on table
            if (deck.isEmpty() || env.config.turnTimeoutMillis <= 0) {
                List<Integer> cards = tableCards();
                if (env.util.findSets(cards, 1).size() == 0)
                    noSets = true;
            }
        }
    }

    /**
     * make list of all cards placed in table and return it
     * 
     * @return list of all cards placed on table
     * 
     * @post - cards.size() == table.countCards()
     */
    private List<Integer> tableCards() {
        List<Integer> cards = new ArrayList<>();
        for (Integer card : table.slotToCard) {
            if (card != null)
                cards.add(card);
        }
        return cards;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        synchronized (table.setsDeclared) {
            try {
                // countdown state or upper timer state
                if (env.config.turnTimeoutMillis >= 0) {
                    long waitingTime = sleepingTill - System.currentTimeMillis();
                    if (table.setsDeclared.isEmpty() && waitingTime > Table.MILLIS_1)
                        table.setsDeclared.wait(waitingTime - Table.MILLIS_1);
                }
                // no timer state
                else if (env.config.turnTimeoutMillis < 0) {
                    if (table.setsDeclared.isEmpty())
                        table.setsDeclared.wait();
                }
            } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     * 
     * @post - the timer display is updated according to current time in the ui.
     */
    private void updateTimerDisplay(boolean reset) {
        // countdown state
        long timeDisplay;
        if (env.config.turnTimeoutMillis > 0) {
            if (reset) {
                warn = false;
                reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
                sleepingTill = reshuffleTime - env.config.turnTimeoutMillis;
                timeDisplay = env.config.turnTimeoutMillis;
            }
            else 
                timeDisplay = reshuffleTime - sleepingTill;
            if (System.currentTimeMillis() >= sleepingTill) {
                if (timeDisplay <= env.config.turnTimeoutWarningMillis) { // warning time
                    warn = true;
                    timeDisplay = reshuffleTime - System.currentTimeMillis();
                    sleepingTill = sleepingTill + Table.MILLIS_10;
                }
                else 
                    sleepingTill = Math.min(sleepingTill + Table.SECOND_BY_MILLIS, reshuffleTime - env.config.turnTimeoutWarningMillis);
                if (timeDisplay >= 0)
                    env.ui.setCountdown(timeDisplay, warn);
                else env.ui.setCountdown(0, warn);
            }
        }
        // upper timer state
        if (env.config.turnTimeoutMillis == 0) {
            if (reset) {
                reshuffleTime = System.currentTimeMillis();
                sleepingTill = reshuffleTime;
                timeDisplay = 0;
            }
            else
                timeDisplay = sleepingTill - reshuffleTime;
            if (System.currentTimeMillis() >= sleepingTill) {
                env.ui.setElapsed(timeDisplay);
                sleepingTill = sleepingTill + Table.SECOND_BY_MILLIS;
            }
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     * 
     * @post - table.countCards() == 0
     */
    void removeAllCardsFromTable() {
        Collections.shuffle(order); // random order
        table.freezeGame = true;
        synchronized (table) { // remove cards and tokens
            for (int i : order) {
                if (table.slotToCard[i] != null) {
                    int card = table.slotToCard[i];
                    removeCardFromTable(i);
                    deck.add(card);
                }
            }
        }
        shuffelCards(); // shuffel deck
    }

    /**
     * Check who is/are the winner/s and displays them.
     * 
     * @pre  - shouldFinish() == true.
     * @post - the winners are displayed in the ui.
     */
    void announceWinners() {
        // find max
        int maxId = 0;
        int counter = 1;
        for (int i = 1; i < players.length; i++) {
            if (players[i].score() > players[maxId].score()) {
                maxId = i;
                counter = 0;
            }
            if (players[i].score() == players[maxId].score())
                counter++;
        }
        // find winners
        int[] winners = new int[counter];
        int index = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i].score() == players[maxId].score()) {
                winners[index] = i;
                index++;
            }
        }
        env.ui.announceWinner(winners);
    }

    /**
     * shuffel the deck
     * 
     * @post - @pre(deck) != deck 
     */
    void shuffelCards() {
        Collections.shuffle(deck);
    }

    /**
     * for testing only!
     * 
     * getter - return new copy
     * 
     * @pre  - none
     * @post - return new copy of deck
     */
    public List<Integer> getDeck() {
        return new ArrayList<>(this.deck);
    }
}