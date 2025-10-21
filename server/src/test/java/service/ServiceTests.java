package service;

import chess.ChessGame;
import model.UserData;
import dataaccess.DataAccessDAO;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

//service tests, no http
public class ServiceTests {
    private UserService userSvc;
    private GameService gameSvc;
    private ClearService clearSvc;

    @BeforeEach
    public void setup() {
        DataAccessDAO db = new DataAccessDAO();
        userSvc = new UserService(db);
        gameSvc = new GameService(db);
        clearSvc = new ClearService(db);
    }

    @Test
    public void clearSuccess() throws Exception {
        var user = new UserData("daniel", "pw", "d@d.com");
        userSvc.register(user);
        clearSvc.clear();
        assertThrows(ServiceException.class, () -> userSvc.login(user));
    }

    @Test
    public void registerSuccess() throws Exception {
        var user = new UserData("bob", "pw", "b@b.com");
        var auth = userSvc.register(user);
        assertNotNull(auth.authToken());
        assertEquals("bob", auth.username());
    }

    @Test
    public void registerDuplicateFail() throws Exception {
        var user = new UserData("ethan", "pw", "e@e.com");
        userSvc.register(user);
        assertThrows(ServiceException.class, () -> userSvc.register(user));
    }

    @Test
    public void loginSuccess() throws Exception {
        var user = new UserData("chris", "pw", "c@c.com");
        userSvc.register(user);
        var auth = userSvc.login(user);
        assertNotNull(auth.authToken());
    }

    @Test
    public void loginFail() throws Exception {
        var user = new UserData("chris", "pw", "c@c.com");
        userSvc.register(user);
        var wrong = new UserData("chris", "wrongpw", "c@c.com");
        assertThrows(ServiceException.class, () -> userSvc.login(wrong));
    }

    @Test
    public void createGameSuccess() throws Exception {
        var user = new UserData("daniel", "pw", "d@d.com");
        var auth = userSvc.register(user);
        int gameID = gameSvc.create(auth.authToken(), "Test");
        assertTrue(gameID > 0);
    }

    @Test
    public void createUnauthorizedGame() {
        assertThrows(ServiceException.class, () -> gameSvc.create("badtoken", "game"));
    }

    @Test
    public void successJoinGame () throws Exception {
        var user = new UserData("daniel", "pw", "d@d.com");
        var auth = userSvc.register(user);
        int gameID = gameSvc.create(auth.authToken(), "JoinTest");
        gameSvc.join(auth.authToken(), ChessGame.TeamColor.WHITE, gameID);
        var list = gameSvc.list(auth.authToken());
        assertEquals(1, list.size());
    }

    @Test
    public void badColor() throws Exception {
        var user = new UserData("daniel", "pw", "d@d.com");
        var auth = userSvc.register(user);
        int gameID = gameSvc.create(auth.authToken(), "ColorTest");
        assertThrows(ServiceException.class, () -> gameSvc.join(auth.authToken(), null, gameID));
    }
}
