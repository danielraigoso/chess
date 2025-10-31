package dataaccess;

import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class DatabaseTests {

    private SqlGameDAO games;

    @BeforeAll
    static void bootstrapSchema() throws Exception {
        DatabaseManager.createDatabase();

        SqlUserDAO.createTable();
        SqlAuthDAO.createTable();
        SqlGameDAO.createTable();
    }
    //commit and push

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

    // clear success
    @Test
    void clear_success() throws Exception {
        var g = games.create("G1");
        assertNotNull(g);
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

    //find(int) pass fail
    @Test
    void find_success() throws Exception {
        var created = games.create("FindMe");
        var found = games.find(created.gameID());
        assertNotNull(found);
        assertEquals(created.gameID(), found.gameID());
        assertEquals("FindMe", found.gameName());
    }

    @Test
    void find_notFound_returnsNull() throws Exception {
        var found = games.find(999_999);
        assertNull(found, "find(nonexistent) should return null");
    }

    // list pass fail
    @Test
    void list_success_multiple() throws Exception {
        games.create("A");
        games.create("B");
        var list = games.list();
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(g -> g.gameName().equals("A")));
        assertTrue(list.stream().anyMatch(g -> g.gameName().equals("B")));
    }

    @Test
    void list_empty_returnsEmpty() throws Exception {
        var list = games.list();
        assertNotNull(list);
        assertTrue(list.isEmpty(), "list() of empty table should be empty");
    }

    // update pass fail
    @Test
    void update_success_changeNamesAndState() throws Exception {
        // Create base game
        var g = games.create("Base");
        int id = g.gameID();

        var users = new SqlUserDAO();
        users.insert(new model.UserData("whitey", "hashed", "w@e.com"));
        users.insert(new model.UserData("blacky", "hashed", "b@e.com"));

        var updated = new GameData(
                id,
                "whitey",
                "blacky",
                "Renamed",
                null
        );

        games.update(updated);

        var after = games.find(id);
        assertNotNull(after);
        assertEquals("Renamed", after.gameName());
        assertEquals("whitey", after.whiteUsername());
        assertEquals("blacky", after.blackUsername());
    }

    @Test
    void update_fail_idNotFound() {
        var bogus = new GameData(
                424242,
                null,
                null,
                "DoesNotExist",
                null
        );
        assertThrows(DataAccessException.class, () -> games.update(bogus));
    }
}

