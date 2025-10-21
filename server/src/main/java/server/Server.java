package server;

import com.google.gson.Gson;
import io.javalin.*;
import model.UserData;
import model.GameData;
import dataaccess.DataAccess;
import dataaccess.DataAccessDAO;
import service.ServiceException;
import service.UserService;
import service.GameService;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();
    private final DataAccess db = new DataAccessDAO();
    private final UserService userSvc = new UserService(db);
    private final service.ClearService clearSvc = new service.ClearService(db);

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        // Register your endpoints and exception handlers here.
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
            ctx.status(200).json(new Empty());
        });
    }

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
}
