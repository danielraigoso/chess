package websocket;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
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

    private final Map<Integer, Set<String>> gameSessions = new ConcurrentHashMap<>();

    private final Map<String, Integer> sessionGame = new ConcurrentHashMap<>();

    private final Map<String, WsContext> activeSessions = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    public void onConnect(WsConnectContext ctx) {
        String sid = ctx.sessionId();
        activeSessions.put(sid,ctx);
        System.out.printf("Websocket connected: %s%n", ctx);
    }

    public void onClose(WsCloseContext ctx){
        String sid = ctx.sessionId();

        Integer gameID = sessionGame.remove(sid);
        if (gameID != null) {
            var set = gameSessions.get(gameID);
            if (set != null) {
                set.remove(sid);
            }
        }

        activeSessions.remove(sid);
        System.out.printf("Websocket closed: %s%n", sid);
    }

    public void onError(WsErrorContext ctx){

        assert ctx.error() != null;
        System.out.printf("Websocket Error (%s): %s%n",
                ctx.sessionId(), ctx.error().getMessage());
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
    private void handleConnect(WsContext ctx, UserGameCommand cmd)
        throws ServiceException, DataAccessException {

        int gameID = cmd.getGameID();
        String auth = cmd.getAuthToken();
        String sid = ctx.sessionId();

        ChessGame game = gameService.loadGameState(auth, gameID);

        sessionGame.put(sid, gameID);
        gameSessions.computeIfAbsent(gameID, id -> ConcurrentHashMap.newKeySet())
                .add(sid);

        ctx.send(gson.toJson(ServerMessage.loadGame(game)));

        String notif = gameService.buildConnectMessage(auth, gameID);
        broadcastToOthers(gameID, sid, ServerMessage.notification(notif));
    }

    private void handleMakeMove(WsContext ctx, UserGameCommand cmd)
            throws ServiceException, DataAccessException, InvalidMoveException {
        int gameID = cmd.getGameID();
        String auth = cmd.getAuthToken();
        ChessMove move = cmd.getMove();
        String sid = ctx.sessionId();

        GameService.MoveResult result = gameService.moveMove(auth, gameID, move);

        ServerMessage loadMsg = ServerMessage.loadGame(result.game());
        broadcastToAll(gameID, loadMsg);

        String moveNotif = result.moveNotif();
        if (moveNotif != null) {
            broadcastToOthers(gameID, sid, ServerMessage.notification(moveNotif));
        }

        String extra = result.extraNotif();
        if (extra != null) {
            broadcastToAll(gameID, ServerMessage.notification(extra));
        }
    }

    private void handleLeave(WsContext ctx, UserGameCommand cmd)
        throws ServiceException, DataAccessException {

        int gameID = cmd.getGameID();
        String auth = cmd.getAuthToken();
        String sid = ctx.sessionId();

        gameService.leaveGame(auth, gameID);

        sessionGame.remove(sid);
        var set = gameSessions.get(gameID);

        if (set != null) {
            set.remove(sid);
        }
        activeSessions.remove(sid);
        String msg = gameService.buildLeaveMessage(auth, gameID);

        broadcastToOthers(gameID, sid, ServerMessage.notification(msg));
    }

    private void handleResign(WsContext ctx, UserGameCommand cmd)
        throws ServiceException, DataAccessException {

        int gameID = cmd.getGameID();
        String auth = cmd.getAuthToken();

        gameService.resignGame(auth, gameID);

        String msg = gameService.buildResignMessage(auth, gameID);

        broadcastToAll(gameID, ServerMessage.notification(msg));
    }

    private void broadcastToAll(int gameID, ServerMessage msg) {
        var sessions = gameSessions.get(gameID);
        if (sessions == null) return;

        String json = gson.toJson(msg);
        for (String sid : sessions) {
            var c = activeSessions.get(sid);
            if (c != null) {
                c.send(json);
            }
        }
    }

    private void broadcastToOthers(int gameID, String senderId, ServerMessage msg) {
        var sessions = gameSessions.get(gameID);
        if (sessions == null){
            return;
        }

        String json = gson.toJson(msg);
        for (String sid : sessions) {
            if (!sid.equals(senderId)) {
                var c = activeSessions.get(sid);
                if (c != null) {
                    c.send(json);
                }
            }
        }
    }
    //hurray
}
