package websocket.messages;

import chess.ChessGame;
import model.UserData;

import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket
 * <p>
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    ServerMessageType serverMessageType;

    private ChessGame game;
    private String message;
    private String errorMessage;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public ChessGame getGame() {
        return game;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    //helpers

    public static ServerMessage loadGame(ChessGame game) {
        ServerMessage msg = new ServerMessage(ServerMessageType.LOAD_GAME);
        msg.game = game;
        msg.message = null;
        msg.errorMessage = null;
        return msg;
    }

    public static ServerMessage notification(String message) {
        ServerMessage msg = new ServerMessage(ServerMessageType.NOTIFICATION);
        msg.game = null;
        msg.message = message;
        msg.errorMessage = null;
        return msg;
    }

    public static ServerMessage error(String errorMessage) {
        ServerMessage msg = new ServerMessage(ServerMessageType.ERROR);
        msg.game = null;
        msg.message = null;
        msg.errorMessage = errorMessage;
        return msg;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage that)) {
            return false;
        }
        return getServerMessageType() == that.getServerMessageType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType());
    }
}
