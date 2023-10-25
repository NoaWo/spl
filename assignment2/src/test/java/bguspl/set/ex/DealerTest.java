package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DealerTest {
    
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Logger logger;

    private Table table;

    private Dealer dealer;

    private Player[] players;

    private Env env;
   
    void assertInvariants() {
        for (int index : dealer.order) {
            assertTrue(index >= 0 && index < env.config.tableSize);
        }
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        env = new Env(logger, new Config(logger, ""), ui, util);
        players = new Player[env.config.players];
        for (int i = 0 ; i < players.length ; i++){
            players[i] = mock(Player.class);
        }
        Integer[] slotToCard = new Integer[env.config.tableSize];
        Integer[] cardToSlot = new Integer[env.config.deckSize];
        table = new Table(env, slotToCard, cardToSlot);
        dealer = new Dealer(env, table, players); 
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void announceWinner_verify() { 

        when(players[0].score()).thenReturn(3);
        when(players[1].score()).thenReturn(1);
        dealer.announceWinners();
        int[] win = new int[1];
        win[0] = 0 ;
        verify(ui).announceWinner(win);
    }

    @Test
    void placeCardsOnTable_verify() {
        
        assertEquals(null, table.slotToCard[0]);
        assertEquals(null, table.slotToCard[2]);
        dealer.placeCardsOnTable();
        assertNotEquals(null, table.slotToCard[0]);
        assertNotEquals(null, table.slotToCard[2]);
        assertTrue(dealer.getDeck().isEmpty() || table.countCards() == env.config.tableSize);
    }

    @Test
    void removeAllCardsFromTable_verify() {       

        dealer.placeCardsOnTable();
        assertNotEquals(null, table.slotToCard[0]);
        assertNotEquals(null, table.slotToCard[2]);
        dealer.removeAllCardsFromTable();
        assertEquals(null, table.slotToCard[0]);
        assertEquals(null, table.slotToCard[2]);
        assertTrue(table.countCards() == 0);
    }

    @Test
    void shuffelCards_verify() {       

        List<Integer> preDeck = dealer.getDeck();
        dealer.shuffelCards();
        assertFalse(dealer.getDeck().equals(preDeck));
    }
}
