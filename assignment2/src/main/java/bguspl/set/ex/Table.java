package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 * @inv tokens[i].size() <= 3 (for 0<=i<tokens.length)
 */
public class Table {

    public class IntList extends ArrayList<Integer> {}

    public static final int SET_SIZE = 3; 

    public static final int FLAG_DEFUALT = -1;
    public static final int FLAG_ILLEGAL_SET = 0;
    public static final int FLAG_LEGAL_SET = 1;
    public static final int FLAG_CANCELLED_SET = 2;

    public static final int SECOND_BY_MILLIS = 1000;
    public static final int MILLIS_10 = 10;
    public static final int MILLIS_1 = 1;

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * list ot tokens for each player
     */
    protected IntList[] tokens; // token by slot

    /**
     * Queue of players' id that declared "set"
     */
    protected Queue<Integer> setsDeclared;

    /**
     * for fair in checking players' set
     */
    protected Semaphore sem;

    /**
     * True only when the dealer replacing all cards on table
     */
    protected volatile boolean freezeGame;
    
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {
        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.tokens = new IntList[env.config.players];
        for (int i = 0; i < tokens.length; i++)
            tokens[i] = new IntList();
        this.setsDeclared = new ArrayBlockingQueue<>(env.config.players, true);
        this.sem = new Semaphore(1, true);
        freezeGame = true;
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {
        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @pre  - slotToCard[@param slot ] == null && cardToSlot[@param card ] == null
     * @post - the card placed is on the table, in the assigned slot.
     */
    public synchronized void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     * 
     * @pre  - slotToCard[@param slot ] != null
     * @post - slotToCard[@param slot ] == null
     */
    public synchronized void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        int card = slotToCard[slot];
        slotToCard[slot] = null;
        cardToSlot[card] = null;
        env.ui.removeCard(slot);
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     * 
     * @pre  - slotToCard[@param slot ] != null && tokens[@param player ].size() < 3
     * @post - tokens[@param player ].contain(@param slot )
     */
    public synchronized void placeToken(int player, int slot) {
        tokens[player].add(slot);
        env.ui.placeToken(player, slot);
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public synchronized boolean removeToken(int player, int slot) {
        Integer s = slot;
        boolean done = tokens[player].remove(s);
        if (done)
            env.ui.removeToken(player, slot);
        return done;
    }

    /**
     * Removes all tokens from a grid slot.
     * @param slot   - the slot from which to remove the tokens.
     */
    public synchronized void removeTokens(int slot) {
        Integer s = slot;
        for (int i = 0; i < tokens.length; i++)
            tokens[i].remove(s);
        env.ui.removeTokens(slot);
    }
}