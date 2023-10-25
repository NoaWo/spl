package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTest {

    Table table;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;
    Env env;
    
    void assertInvariants() {
        for (int i = 0; i < env.config.deckSize; i++) {
            Integer slot = table.cardToSlot[i];
            if (slot != null)
                assertEquals(i, table.slotToCard[slot]);
        }
        for (int i = 0; i < env.config.tableSize; i++) {
            Integer card = table.slotToCard[i];
            if (card != null)
                assertEquals(i, table.cardToSlot[card]);
        }
        for (int i = 0; i < table.tokens.length; i++) {
            assertTrue(table.tokens[i].size() <= 3);
        }
    }

    @BeforeEach
    void setUp() {

        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        MockLogger logger = new MockLogger();
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];

        env = new Env(logger, config, new MockUserInterface(), new MockUtil());
        table = new Table(env, slotToCard, cardToSlot);
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    private int fillSomeSlots() {
        slotToCard[1] = 3;
        slotToCard[2] = 5;
        cardToSlot[3] = 1;
        cardToSlot[5] = 2;

        return 2;
    }

    private void fillAllSlots() {
        for (int i = 0; i < slotToCard.length; ++i) {
            slotToCard[i] = i;
            cardToSlot[i] = i;
        }
    }

    private void placeSomeCardsAndAssert() throws InterruptedException {
        table.removeCard(2);
        table.placeCard(8, 2);

        assertEquals(8, (int) slotToCard[2]);
        assertEquals(2, (int) cardToSlot[8]);
    }

    private void removeSomeCardsAndAssert() {
        
        table.removeCard(1);
        assertEquals(null, cardToSlot[3]);
        assertEquals(null, slotToCard[1]);
    }

    private void placeSome_tokens() {
        
        table.placeToken(0, 4);
    }

    @Test
    void countCards_NoSlotsAreFilled() {

        assertEquals(0, table.countCards());
    }

    @Test
    void countCards_SomeSlotsAreFilled() {

        int slotsFilled = fillSomeSlots();
        assertEquals(slotsFilled, table.countCards());
    }

    @Test
    void countCards_AllSlotsAreFilled() {

        fillAllSlots();
        assertEquals(slotToCard.length, table.countCards());
    }

    @Test
    void placeCard_SomeSlotsAreFilled() throws InterruptedException {

        fillSomeSlots();
        placeSomeCardsAndAssert();
    }

    @Test
    void placeCard_AllSlotsAreFilled() throws InterruptedException {
        fillAllSlots();
        placeSomeCardsAndAssert();
    }
    
    @Test
    void removeCard_some_removes() {
        fillSomeSlots();
        removeSomeCardsAndAssert();
    }

    @Test
    void placetoken_some_tokens() {
        placeSome_tokens();
        int j = table.tokens[0].remove(0);
        assertEquals(4, j);
    }

    @Test
    void removetoken_some_tokens() {
        placeSome_tokens();
        table.removeToken(0, 4);
        boolean bool = table.tokens[0].isEmpty();
        assertTrue(bool);
    }

    static class MockUserInterface implements UserInterface {
        @Override
        public void dispose() {}
        @Override
        public void placeCard(int card, int slot) {}
        @Override
        public void removeCard(int slot) {}
        @Override
        public void setCountdown(long millies, boolean warn) {}
        @Override
        public void setElapsed(long millies) {}
        @Override
        public void setScore(int player, int score) {}
        @Override
        public void setFreeze(int player, long millies) {}
        @Override
        public void placeToken(int player, int slot) {}
        @Override
        public void removeTokens() {}
        @Override
        public void removeTokens(int slot) {}
        @Override
        public void removeToken(int player, int slot) {}
        @Override
        public void announceWinner(int[] players) {}
    };

    static class MockUtil implements Util {
        @Override
        public int[] cardToFeatures(int card) {
            return new int[0];
        }

        @Override
        public int[][] cardsToFeatures(int[] cards) {
            return new int[0][];
        }

        @Override
        public boolean testSet(int[] cards) {
            return false;
        }

        @Override
        public List<int[]> findSets(List<Integer> deck, int count) {
            return null;
        }

        @Override
        public void spin() {}
    }

    static class MockLogger extends Logger {
        protected MockLogger() {
            super("", null);
        }
    }
}