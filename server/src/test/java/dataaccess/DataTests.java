package dataaccess;

import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class DataTests {

    private SqlGameDAO games;

    @BeforeAll
    static void bootstrapSchema() throws Exception {
        DatabaseManager.createDatabase();

        SqlUserDAO.createTable();
        SqlAuthDAO.createTable();
        SqlGameDAO.createTable();
    }

    @BeforeEach
    void setUp() throws Exception {
        games = new SqlGameDAO();
        try {
            new SqlAuthDAO().clear();
            new SqlGameDAO().clear();
            new SqlUserDAO().clear();
        } catch (DataAccessException dae) {
            throw dae;
        }
    }

    @Test
    void clear_success() throws Exception {
        // seed one game
        var g = games.create("G1");
        assertNotNull(g);
        // clear
        games.clear();
        Collection<GameData> all = games.list();
        assertTrue(all.isEmpty(), "clear() should remove all rows");
    }

    //create(String) pass/fail
    @Test
    void create_success() throws Exception {
        var g = games.create("MyGame");
        assertNotNull(g);
        assertTrue(g.gameID() > 0);
        assertEquals("MyGame", g.gameName());
        assertNull(g.whiteUsername());
        assertNull(g.blackUsername());
        assertNull(g.game());
    }

    @Test
    void create_fail() {
        assertThrows(DataAccessException.class, () -> games.create(""));
    }

    @Test
    void create_fail_nullName() {
        assertThrows(DataAccessException.class, () -> games.create(null));
    }
}

