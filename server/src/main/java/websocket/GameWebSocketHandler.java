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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler {

    private final Gson gson = new Gson();
    private final GameService gameService;

    private final Map<Integer, Set<WsContext> gameSessions = new ConcurrentHashMap<>();

    private final Map<WsContext, Integer> sessionGame = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void onConnect(WsConnectContext ctx) {

    }

    public void onClose(WsCloseContext ctx){
            removeFromGame(ctx);
    }
    public void onError(WsErrorContext ctx){
            removeFromeGame(ctx);
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            var command = gson.fromJson(ctx.message(),UserGameCommand.class);
            switch (command.getCommandType()) {
                case CONNECT -> handleConnect(ctx, command);
                case MAKE_MOVE -> handleMakeMove(ctx,command);
                case LEAVE -> handleLeave(ctx,command);
                case RESIGN -> handleResign(ctx,command);
            }
        } catch (ServiceException | DataAccessException ex) {
            ctx.send(gson.toJson(ServerMessage.error(ex.getMessage())));
        } catch (Exception ex) {
            ctx.send(gson.toJson(ServerMessage.error("Error: " + ex.getMessage())));
        }
    }

    //helper handlers
}
