package client;

import chess.ChessGame;
import model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class ServerFacade {
    private final String baseUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    // register
    public AuthData register(String username, String password, String email) throws IOException, InterruptedException {
        var user = new UserData(username, password, email);
        var body = gson.toJson(user);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/user"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }

        return gson.fromJson(response.body(), AuthData.class);
    }


    // login
    public AuthData login(String username, String password) throws IOException, InterruptedException {
        var login = new LoginRequest(username, password);
        var body = gson.toJson(login);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/session"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }

        return gson.fromJson(response.body(), AuthData.class);
    }

    public void logout(String authToken) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/session"))
                .header("Authorization", authToken)
                .DELETE()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }
    }

    public GameData[] listGames(String authToken) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game"))
                .header("Authorization", authToken)
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }

        record ListGamesResponse(GameData[] games) {
        }
        var wrapper = gson.fromJson(response.body(), ListGamesResponse.class);
        return wrapper.games();
    }

    public GameData createGame(String authToken, String name) throws IOException, InterruptedException {
        var create = new CreateGameRequest(name);
        var body = gson.toJson(create);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game"))
                .header("Authorization", authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }
        return gson.fromJson(response.body(), GameData.class);
    }

    public void joinGame(String authToken, ChessGame.TeamColor color, int gameID) throws IOException, InterruptedException {
        var join = new JoinGameRequest(color, gameID);
        var body = gson.toJson(join);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/game"))
                .header("Authorization", authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }
    }

    public String getWsUrl() {
        return baseUrl.replaceFirst("^http", "ws") + "/ws";
    }
    //hurray
}