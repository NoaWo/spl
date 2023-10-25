package bguspl.set.ex;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import bguspl.set.Env;

import java.util.Random;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * Queue of slots from input
     */
    protected BlockingQueue<Integer> actions;

    /**
     * True iff the player should be freeze
     */
    private boolean freeze;
    
    /**
     * The flag holds the feedback from dealer.
     */
    private volatile int flag; 

    /**
     * The dealer of the game.
     */
    private Dealer dealer;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.actions = new ArrayBlockingQueue<>(Table.SET_SIZE, true);
        freeze = false;
        flag = Table.FLAG_DEFUALT;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // main player loop
            try {
                int slot;
                synchronized (actions) {
                    if (table.freezeGame) {
                        actions.clear();
                        if (!human) actions.notifyAll();
                    }
                    while (actions.isEmpty() || freeze)
                        actions.wait();
                    slot = actions.remove();
                    if (!human) actions.notifyAll();
                }
                boolean done = table.removeToken(id, slot);
                if (!done) {
                    table.sem.acquire();
                    synchronized (table.setsDeclared) {
                        synchronized (table) {
                            if (table.slotToCard[slot] != null && table.tokens[id].size() < Table.SET_SIZE) {
                                table.placeToken(id, slot);
                                if (table.tokens[id].size() == Table.SET_SIZE) { // if third token
                                    freeze = true;
                                    table.setsDeclared.add(id);
                                    table.setsDeclared.notifyAll();
                                }
                            }
                        }
                    }
                    table.sem.release();
                }
                if (freeze) // declared set
                    getFeedback();
            } catch (InterruptedException ignored) {}
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * get feedback from dealer and perform related action
     * 
     * @pre  - flag == Table.FLAG_DEFUALT
     * @pre  - freeze == true
     * @post - flag == Table.FLAG_DEFUALT
     * @post - freeze == false
     */
    private synchronized void getFeedback() {
        try {
            while (flag == Table.FLAG_DEFUALT) // wait for feedback
                this.wait();
            if (flag == Table.FLAG_LEGAL_SET) // legal set
                point();
            else if (flag == Table.FLAG_ILLEGAL_SET) // illegal set
                penalty();
            else
                unFreeze();
            if (!human) this.notifyAll();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            Random rand = new Random();
            while (!terminate) {
                // player key press simulator
                try {
                    int slot = rand.nextInt(env.config.tableSize);
                    synchronized (dealer) {
                        while (table.freezeGame) // sleep if game is freezing now
                            dealer.wait();
                    }
                    synchronized (actions) {
                        while (actions.remainingCapacity() == 0) // sleep if actions queue is full
                            actions.wait();
                    }
                    synchronized (Player.this) {
                        while (freeze) // sleep if player is in freeze state
                            Player.this.wait();
                    }
                    Player.this.keyPressed(slot);
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     *
     * @pre  - terminate == false
     * @post - terminate == true
     */
    public void terminate() {
        terminate = true;
        if (!human)
            aiThread.interrupt();
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     * 
     * @post - if (!freeze) actions.contain(slot) == true
     */
    public void keyPressed(int slot) {
        if (!freeze && !table.freezeGame) {
            try {
                synchronized (actions) {
                    actions.add(slot);
                    actions.notifyAll();
                }
           } catch (IllegalStateException ignored) {}
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        setFreeze(env.config.pointFreezeMillis);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        setFreeze(env.config.penaltyFreezeMillis);
    }

    /**
     * sending the player thread to sleep to @param freezeTime millisecond
     * 
     * @param freezeTime - the amount time in millisecond that the thread sould sleep
     * 
     * @pre  - @param freezeTime >= 0
     */
    public void setFreeze(long freezeTime) {
        try {
            while (freezeTime > 0) {
                env.ui.setFreeze(id, freezeTime);
                Thread.sleep(Math.min(freezeTime, Table.SECOND_BY_MILLIS));
                freezeTime = freezeTime - Table.SECOND_BY_MILLIS;
            }
            env.ui.setFreeze(id, 0);
            unFreeze();
        } catch (InterruptedException ignored) {}
    }

    /**
     * simple getter
     * 
     * @pre  - none
     * @post - trivial
     */
    public int score() {
        return score;
    }

    /**
     * change player state to unfreeze 
     * 
     * @pre  - freeze == true
     * @post - freeze == false
     * @post - flag == Table.FLAG_DEFUALT
     * @post - actions.isEmpty()
     */
    public void unFreeze() {
        flag = Table.FLAG_DEFUALT;
        synchronized (actions) {
            freeze = false;
            actions.clear();
            actions.notifyAll();
        }
    }

    /**
     * simple setter of flag 
     * 
     * @param flag - the new flag
     * 
     * @pre  - @param flag is a valid flag
     * @post - flag == @param flag
     */
    public void setChecked(int flag) {
        this.flag = flag;
    }

    /**
     * simple getter
     * 
     * @pre  - none
     * @post - trivial
     */
    public Thread getPlayerThread() {
        return playerThread;
    }

    /**
     * for testing only!
     * 
     * simple setter of freeze 
     *  
     * @param freeze - the new value for freeze
     * 
     * @pre  - none
     * @post - freeze == @param freeze
     */
    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
    }

    /**
     * for testing only!
     * 
     * simple getter
     * 
     * @pre  - none
     * @post - trivial
     */
    public boolean getFreeze() {
        return this.freeze;
    }

    /**
     * for testing only!
     * 
     * simple getter
     * 
     * @pre  - none
     * @post - trivial
     */
    public int getFlag() {
        return this.flag;
    }
}