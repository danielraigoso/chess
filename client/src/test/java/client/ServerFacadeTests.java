package client;

import org.junit.jupiter.api.*;
import server.Server;

import model.*;
import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @Test
    public void clearDb() throws Exception{
        facade.clear();
    }

    // register

    @Test
    void registerSuccess() throws Exception {
        AuthData auth = facade.register("player1", "password", "player1@email.com");
        assertNotNull(auth);
        assertEquals("player1", auth.username());
        assertNotNull(auth.authToken());
        assertTrue(auth.authToken().length() > 5);
    }

    @Test
    void registerDuplicateFail() throws Exception {
        facade.register("player1", "password", "player1@email.com");

        var thrown = assertThrows(Exception.class, () ->
                facade.register("player1", "password", "player1@email.com")
                );
        assertTrue(thrown.getMessage().toLowerCase().contains("error"));
    }

    // login

    @Test
    void loginSuccess() throws Exception {
        facade.register
    }

}
