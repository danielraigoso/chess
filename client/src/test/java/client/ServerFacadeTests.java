package client;

import chess.ChessGame;
import org.junit.jupiter.api.*;
import server.Server;

import model.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static int port;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @BeforeEach
    public void clearDb() throws Exception{
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/db"))
                .DELETE()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // register

    @Test
    void registerSuccess() throws Exception {
        AuthData auth = facade.register("chris", "password", "chris@email.com");
        assertNotNull(auth);
        assertEquals("chris", auth.username());
        assertNotNull(auth.authToken());
        assertTrue(auth.authToken().length() > 5);
    }

    @Test
    void registerDuplicateFail() throws Exception {
        facade.register("ethan", "password", "ethan@email.com");
        assertThrows(RuntimeException.class, () ->
                facade.register("ethan", "password", "ethan@gmail.com")
                );
    }

    // login

    @Test
    void loginSuccess() throws Exception {
        facade.register("heidi", "some", "heidi@email.com");
        AuthData auth = facade.login("heidi", "some");
        assertNotNull(auth);
        assertEquals("heidi", auth.username());
    }

    @Test
    void loginFail() throws Exception {
        facade.register("heidi", "some", "heidi@email.com");
        assertThrows(RuntimeException.class, () -> facade.login("heidi", "thing"));
    }

    // logout

    @Test
    void logoutSuccess() throws Exception {
        AuthData auth = facade.register("heidi", "some", "heidi@email.com");
        facade.logout(auth.authToken());

        assertThrows(RuntimeException.class, () -> facade.logout("bad-token"));
    }

    // list games

    @Test
    void listGameSuccess() throws Exception {
        AuthData auth = facade.register("heidi", "some", "heidi@email.com");
        facade.createGame(auth.authToken(), "game1");
        facade.createGame(auth.authToken(), "game2");

        GameData[] games = facade.listGames(auth.authToken());
        assertNotNull(games);
        assertTrue(games.length >= 2);
    }

    @Test
    void listGameBad() throws Exception {
        assertThrows(RuntimeException.class, () -> facade.listGames("bad-token"));
    }

    // create game

    @Test
    void createGameSuccess() throws Exception {
        AuthData auth = facade.register("heidi", "some", "heidi@email.com");
        GameData game = facade.createGame(auth.authToken(), "my game");
        assertNotNull(game);
        assertEquals("my game", game.gameName());
        assertTrue(game.gameID() > 0);
    }


}
