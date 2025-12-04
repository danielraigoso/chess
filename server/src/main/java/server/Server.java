package server;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.*;
import model.UserData;
import model.GameData;
import dataaccess.DataAccess;
import dataaccess.SqlDataAccessDAO;
import service.ServiceException;
import service.UserService;
import service.GameService;
import websocket.GameWebSocketHandler;
import java.time.Duration;

import javax.swing.*;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();
    private final DataAccess db = new SqlDataAccessDAO();
    private final UserService userSvc = new UserService(db);
    private final service.ClearService clearSvc = new service.ClearService(db);
    private final GameService gameSvc = new GameService(db);

    public Server() {
        javalin = Javalin.create(config -> {
            config.staticFiles.add("web");
            config.jetty.modifyWebSocketServletFactory(wsFactory -> {
                wsFactory.setIdleTimeout(Duration.ofMinutes(30));
            });
        });
        // Register your endpoints and exception handlers here.

        // Exception handlers that always return a JSON { "message": ... }
        javalin.exception(ServiceException.class, (e, ctx) -> {
            ctx.status(e.statusCode());
            ctx.contentType("application/json");
            ctx.result(gson.toJson(new Message(e.getMessage())));
        });
        javalin.exception(DataAccessException.class, (e, ctx) -> {
            ctx.status(500);
            ctx.contentType("application/json");
            ctx.result(gson.toJson(new Message("Error: " + e.getMessage())));
        });
        // Register POST
        javalin.post("/user", ctx -> {
            try {
                var req = gson.fromJson(ctx.body(), UserData.class);
                var auth = userSvc.register(req);
                ctx.status(200).result(gson.toJson(new AuthOut(auth.username(), auth.authToken())));
            } catch (ServiceException se) {
                ctx.status(se.statusCode()).result(gson.toJson(new Message(se.getMessage())));
            }
        });
        // login  POST
        javalin.post("/session", ctx -> {
            try {
                var req = gson.fromJson(ctx.body(), UserData.class);
                var auth = userSvc.login(req);
                ctx.status(200).result(gson.toJson(new AuthOut(auth.username(), auth.authToken())));
            } catch (ServiceException se) {
                ctx.status(se.statusCode()).result(gson.toJson(new Message(se.getMessage())));
            }
        });
        // logout DELETE
        javalin.delete("/session", ctx -> {
            try {
                var token = ctx.header("authorization");
                userSvc.logout(token);
                ctx.status(200).result(gson.toJson(new Empty()));
            } catch (ServiceException se) {
                ctx.status(se.statusCode()).result(gson.toJson(new Message(se.getMessage())));
            }
        });
        javalin.delete("/db", ctx -> {
            clearSvc.clear();
            ctx.status(200).result(gson.toJson(new Empty()));
        });
        // game stuff
        javalin.post("/game", ctx -> {
            try {
                var token = ctx.header("authorization");
                var req = gson.fromJson(ctx.body(), CreateReq.class);
                int gameID = gameSvc.create(token, req.gameName());
                ctx.status(200).result(gson.toJson(new CreateRes(gameID)));
            } catch (ServiceException se) {
                ctx.status(se.statusCode()).result(gson.toJson(new Message(se.getMessage())));
            }
        });
        // get game
        javalin.get("/game", ctx -> {
            try {
                var token = ctx.header("authorization");
                var games = gameSvc.list(token);
                ctx.status(200).result(gson.toJson(new ListRes(games)));
            } catch (ServiceException se) {
                ctx.status(se.statusCode()).result(gson.toJson(new Message(se.getMessage())));
            }
        });
        //put
        javalin.put("/game", ctx -> {
            try {
                var token = ctx.header("authorization");
                var req = gson.fromJson(ctx.body(), JoinReq.class);
                var color = parseColor(req.playerColor());
                gameSvc.join(token, color, req.gameID());
                ctx.status(200).result(gson.toJson(new Empty()));
            } catch (ServiceException se) {
                    ctx.status(se.statusCode()).result(gson.toJson(new Message(se.getMessage())));
            }
        });
        //websocket stuff?
        var wsHandler = new GameWebSocketHandler(gameSvc);
        javalin.ws("/ws", ws -> {
            ws.onConnect(wsHandler::onConnect);
            ws.onClose(wsHandler::onClose);
            ws.onError(wsHandler::onError);
            ws.onMessage(wsHandler::onMessage);
        });
    }
    //hurray
    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }

    private record Message(String message) {}
    private record AuthOut(String username, String authToken) {}
    private record Empty() {}
    private record CreateReq(String gameName) {}
    private record CreateRes(Integer gameID) {}
    private record ListRes(java.util.Collection<GameData> games) {}
    private record JoinReq(String playerColor, Integer gameID) {}

    private static chess.ChessGame.TeamColor parseColor(String s) {
        if (s == null){
            return null;
        }
        try {return chess.ChessGame.TeamColor.valueOf(s);}
        catch (IllegalArgumentException e) {return null;}
    }
}
