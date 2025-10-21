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

    // clear
    @Test
    public void clearSuccess() throws Exception {
        var user = new UserData("daniel", "pw", "d@d.com");
        userSvc.register(user);
        clearSvc.clear();
        assertThrows(ServiceException.class, () -> userSvc.login(user));
    }

    //register success and fail
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

    //login success and fail
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

    //logout success and fail
    @Test
    public void loutOutSuccess() throws Exception {
        var user = new UserData("chris", "pw", "c@c.com");
        var auth = userSvc.register(user);
        var ex = assertThrows(ServiceException.class, () -> userSvc.logout(auth.authToken()));
        assertEquals(401, ex.statusCode());
    }

    @Test
    public void logOutFail() throws Exception {
        var ex = assertThrows(ServiceException.class, () -> userSvc.logout("badtoken"));
        assertEquals(401, ex.statusCode());
    }

    //create game success and fail
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

    // join game success and failure
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

    //list games success and  fail
    @Test
    public void listGamesSuccess() throws Exception {
        var user = new UserData("daniel", "pw", "d@d.com");
        var auth = userSvc.register(user);
        // create a couple of games
        gameSvc.create(auth.authToken(), "Game1");
        gameSvc.create(auth.authToken(), "Game2");
        var games = gameSvc.list(auth.authToken());
        assertTrue(games.size() >= 2);
    }

    @Test
    public void listGamesUnauthorizedFail() {
        var ex = assertThrows(ServiceException.class, () -> gameSvc.list("invalid"));
        assertEquals(401, ex.statusCode());
    }
}
