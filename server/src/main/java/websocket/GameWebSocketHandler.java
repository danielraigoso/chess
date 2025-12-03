package websocket;

import chess.ChessGame;
import chess.ChessMove;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import io.javalin.websocket.*;
import service.GameService;
import service.ServiceException;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import javax.xml.crypto.Data;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler {

    private final Gson gson = new Gson();
    private final GameService gameService;

    private final Map<Integer, Set<WsContext>> gameSessions = new ConcurrentHashMap<>();
    private final Map<WsContext, Integer> sessionGame = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void onConnect(WsConnectContext ctx) {
        System.out.printf("Websocket connected: %s%n", ctx.getSessionID());
    }

    public void onClose(WsCloseContext ctx){
        WsContext ws = ctx;
        Integer gameID = sessionGame.remove(ws);
        if (gameID != null) {
            var set = gameSessions.get(gameID);
            if (set != null) {
                set.remove(ws);
            }
        }

        System.out.printf("Websocket closed: %s%n", ctx.getSessionID());
    }

    public void onError(WsErrorContext ctx){

        System.out.printf
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            var command = gson.fromJson(ctx.message(),UserGameCommand.class);
            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                // case MAKE_MOVE -> handleMakeMove(ctx,command);
                // case LEAVE -> handleLeave(ctx,command);
                // case RESIGN -> handleResign(ctx,command);
            }
        } catch (ServiceException | DataAccessException ex) {
            ctx.send(gson.toJson(ServerMessage.error(ex.getMessage())));
        } catch (Exception ex) {
            ctx.send(gson.toJson(ServerMessage.error("Error: " + ex.getMessage())));
        }
    }

    //helper handlers
    private void handleConnect(Session session, UserGameCommand cmd)
        throws ServiceException, DataAccessException {

        int gameID = cmd.getGameID();
        String auth = cmd.getAuthToken();


    }
}
