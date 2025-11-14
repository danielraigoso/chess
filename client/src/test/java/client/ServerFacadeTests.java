package client;

import chess.ChessGame;
import org.junit.jupiter.api.*;
import server.Server;

import model.*;

import java.lang.reflect.Array;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

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
        //push
    @Test
    void logoutBad() {
        var thrown = assertThrows(Exception.class, () -> facade.logout("bad-token")
        );
        assertTrue(thrown.getMessage().toLowerCase().contains("error"));
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
        assertTrue(game.gameID() > 0);
        GameData[] games = facade.listGames(auth.authToken());
        GameData found = Arrays.stream(games)
                .filter(g -> g.gameID() == game.gameID())
                .findFirst()
                .orElse(null);
        assertNotNull(found);
        assertEquals("my game", found.gameName());
    }

    @Test
    void createGameBad() throws Exception {
        assertThrows(RuntimeException.class, () -> facade.createGame("bad-token", "naw"));
    }


    // join game

    @Test
    void joinGameSuccess() throws Exception {
        AuthData auth = facade.register("heidi", "some", "heidi@email.com");
        GameData create = facade.createGame(auth.authToken(), "join");

        facade.joinGame(auth.authToken(), ChessGame.TeamColor.WHITE, create.gameID());

        GameData[] games = facade.listGames(auth.authToken());
        GameData joined = null;
        for (GameData g : games) {
            if (g.gameID() == create.gameID()) {
                joined = g;
                break;
            }
        }

        assertNotNull(joined);
        assertEquals("heidi", joined.whiteUsername());
    }

    @Test
    void joinGameBad() throws Exception {
        AuthData auth = facade.register("heidi", "some", "heidi@email.com");
        GameData create = facade.createGame(auth.authToken(), "bad");

        assertThrows(RuntimeException.class, () -> facade.joinGame("bad-token", ChessGame.TeamColor.WHITE, create.gameID()));
    }
}
