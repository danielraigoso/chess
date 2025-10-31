package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;
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
    void clearSuccess() throws Exception {
        var g = games.create("G1");
        assertNotNull(g);
        games.clear();
        Collection<GameData> all = games.list();
        assertTrue(all.isEmpty(), "clear() should remove all rows");
    }

    //create(String) pass/fail
    @Test
    void createSuccess() throws Exception {
        var g = games.create("MyGame");
        assertNotNull(g);
        assertTrue(g.gameID() > 0);
        assertEquals("MyGame", g.gameName());
        assertNull(g.whiteUsername());
        assertNull(g.blackUsername());
        assertNull(g.game());
    }

    @Test
    void createFail() {
        assertThrows(DataAccessException.class, () -> games.create(""));
    }

    @Test
    void createFailNullName() {
        assertThrows(DataAccessException.class, () -> games.create(null));
    }

    //find(int) pass fail
    @Test
    void findSuccess() throws Exception {
        var created = games.create("FindMe");
        var found = games.find(created.gameID());
        assertNotNull(found);
        assertEquals(created.gameID(), found.gameID());
        assertEquals("FindMe", found.gameName());
    }

    @Test
    void findNotFoundReturnsNull() throws Exception {
        var found = games.find(999_999);
        assertNull(found, "find(nonexistent) should return null");
    }

    // list pass fail
    @Test
    void listSuccessMultiple() throws Exception {
        games.create("A");
        games.create("B");
        var list = games.list();
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(g -> g.gameName().equals("A")));
        assertTrue(list.stream().anyMatch(g -> g.gameName().equals("B")));
    }

    @Test
    void listEmptyReturnsEmpty() throws Exception {
        var list = games.list();
        assertNotNull(list);
        assertTrue(list.isEmpty(), "list() of empty table should be empty");
    }

    // update pass fail
    @Test
    void updateSuccessChangeNamesAndState() throws Exception {
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
    void updateFailIdNotFound() {
        var bogus = new GameData(
                424242,
                null,
                null,
                "DoesNotExist",
                null
        );
        assertThrows(DataAccessException.class, () -> games.update(bogus));
    }

    //user DAO tests

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class SqlUserDAOTests {

        private SqlUserDAO users;

        @BeforeAll
        static void bootstrapSchema() throws Exception {
            DatabaseManager.createDatabase();
            SqlUserDAO.createTable();
        }

        @BeforeEach
        void setUp() throws Exception {
            users = new SqlUserDAO();
            users.clear();
        }

        // clear()
        @Test
        void clearSuccess() throws Exception {
            users.insert(new UserData("alice", "$2a$10$hash", "a@a.com"));
            users.clear();
            assertNull(users.find("alice"));
        }

        // insert
        @Test
        void insertSuccess() throws Exception {
            String plain = "pw123";
            var u = new UserData("bob", plain, "b@b.com"); // plaintext in
            users.insert(u);
            var found = users.find("bob");
            assertNotNull(found);
            assertEquals("bob", found.username());
            assertEquals("b@b.com", found.email());
            assertTrue(BCrypt.checkpw(plain, found.password()));
            assertNotEquals(plain, found.password());
        }

        @Test
        void insert_duplicate_fails() throws Exception {
            var u1 = new UserData("dup", "$2a$10$hash1", "d1@d.com");
            var u2 = new UserData("dup", "$2a$10$hash2", "d2@d.com");
            users.insert(u1);
            assertThrows(DataAccessException.class, () -> users.insert(u2));
        }

        //find(String)

        @Test
        void findSuccess() throws Exception {
            users.insert(new UserData("carol", "$2a$10$hash", "c@c.com"));
            var found = users.find("carol");
            assertNotNull(found);
            assertEquals("carol", found.username());
        }

        @Test
        void findNotFoundNull() throws DataAccessException {
            assertNull(users.find("ghost"));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.DisplayName.class)
    class SqlAuthDAOTests {

        private SqlAuthDAO auths;

        @BeforeAll
        static void bootstrapSchema() throws Exception {
            DatabaseManager.createDatabase();
            SqlAuthDAO.createTable();
        }

        @BeforeEach
        void setUp() throws Exception {
            auths = new SqlAuthDAO();
            SqlUserDAO users = new SqlUserDAO();
            auths.clear();
            users.clear();

            users.insert(new UserData("user1", "pw", "u1@e.com"));
            users.insert(new UserData("user2", "pw", "u2@e.com"));
        }

        // clear()
        @Test
        void clearSuccess() throws Exception {
            auths.insert(new AuthData("tok1", "user1"));
            auths.clear();
            assertNull(auths.find("tok1"));
        }

        // insert(AuthData)
        @Test
        void insertSuccess() throws Exception {
            var a = new AuthData("t123", "user1");
            auths.insert(a);
            var found = auths.find("t123");
            assertNotNull(found);
            assertEquals("user1", found.username());
        }

        @Test
        void insertDuplicateFails() throws Exception {
            auths.insert(new AuthData("dupTok", "user1"));
            assertThrows(DataAccessException.class, () -> auths.insert(new AuthData("dupTok", "user2")));
        }

        // find(String)
        @Test
        void findSuccess() throws Exception {
            auths.insert(new AuthData("tk", "user2"));
            var found = auths.find("tk");
            assertNotNull(found);
            assertEquals("user2", found.username());
        }

        @Test
        void findNotFoundNull() throws Exception {
            assertNull(auths.find("none"));
        }

        // delete(String)
        @Test
        void deleteSuccess() throws Exception {
            auths.insert(new AuthData("gone", "user1"));
            auths.delete("gone");
            assertNull(auths.find("gone"));
        }

        @Test
        void deleteMissing() throws Exception {
            auths.delete("missing");
            assertNull(auths.find("missing"));
        }
    }
}

