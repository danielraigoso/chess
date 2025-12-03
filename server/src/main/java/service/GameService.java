package service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import chess.ChessGame;
import chess.ChessMove;
import chess.InvalidMoveException;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;

import javax.xml.crypto.Data;


public class GameService {
    private final DataAccess db;

    public GameService(DataAccess db) {
        this.db = db;
    }

    //Post, creates a new game

    public int create(String authToken, String gameName)
            throws ServiceException, DataAccessException {
        requireAuth(authToken);
        if (isBlank(gameName)) {
            throw new ServiceException(400, "Error: bad request");
        }
        try {
            var game = db.games().create(gameName);
            return game.gameID();
        } catch (DataAccessException dae) {
            throw new ServiceException(500, "Error: " + dae.getMessage());
        }
    }

    //Get game

    public Collection<GameData> list(String authToken) throws ServiceException, DataAccessException {
        requireAuth(authToken);
        return new ArrayList<>(db.games().list());
    }

    //put game, join as white/black

    public void join(String authToken, ChessGame.TeamColor color, Integer gameID) throws ServiceException, DataAccessException {
        var username = requireAuth(authToken);
        if (color == null || gameID == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        var game = db.games().find(gameID);

        if (game == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        switch (color) {
            case WHITE -> {
                if (game.whiteUsername() != null & !Objects.equals(game.whiteUsername(), username)) {
                    throw new ServiceException(403, "Error: already taken");
                }
                var updated = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
                try {db.games().update(updated);}
                catch (DataAccessException dae) {
                    throw new ServiceException(500, "Error: " + dae.getMessage());
                }
            }
            case BLACK -> {
                if (game.blackUsername() != null && !Objects.equals(game.blackUsername(), username)) {
                    throw new ServiceException(403, "Error: already taken");
                }
                var updated = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
                try {db.games().update(updated);}
                catch (DataAccessException dae) {
                    throw new ServiceException(500, "Error: " + dae.getMessage());
                }
            }
        }
    }

    //phase 6 websocket helps
    public ChessGame loadGameState(String authToken, int gameID) throws ServiceException, DataAccessException {

        var gameData = db.games().find(gameID);
        if (gameData == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        ChessGame game = gameData.game();
        if (game == null) {
            game = new ChessGame();
            game.getBoard().resetBoard();

            var updated = new GameData(
                    GameData.gameID(),
                    GameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    game
            );
            db.games().update(updated);
        }
        return game;
    }

    public record MoveResult(ChessGame game, String moveNotif, String extraNotif){}

    public MoveResult moveMove(String authToken, int gameID, ChessMove move)
            throws ServiceException, DataAccessException, InvalidMoveException {

        String username = requireAuth(authToken);

        var gameData = db.games().find(gameID);
        if(gameData == null) {
            throw new ServiceException(400, "Error: bad request");
        }

        ChessGame game = gameData.game();
        if (game == null) {
            game = new ChessGame();
            game.getBoard().resetBoard();
        }

        if (gameData.whiteUsername() == null || gameData.blackUsername() == null ||
                game.isInCheckmate(ChessGame.TeamColor.WHITE) ||
                game.isInCheckmate(ChessGame.TeamColor.BLACK) ||
                game.isInStalemate(ChessGame.TeamColor.WHITE) ||
                game.isInStalemate(ChessGame.TeamColor.BLACK)) {
            throw new ServiceException(400, "Error: game over");
        }

        ChessGame.TeamColor playerColor;
        if (username.equals(gameData.whiteUsername())) {
            playerColor = ChessGame.TeamColor.WHITE;
        } else if (username.equals(gameData.blackUsername())) {
            playerColor = ChessGame.TeamColor.BLACK;
        } else {
            throw new ServiceException(400, "Error: not a player in the game");
        }

        if (game.getTeamTurn() != playerColor) {
            throw new ServiceException(400, "Error: wrong turn");
        }

        try {
            game.makeMove(move);
        } catch (InvalidMoveException e) {
            throw new ServiceException(400, "Error: invalid move");
        }

        var updated = new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        );

        db.games().update(updated);

        String moveText = buildMoveDescription(username, move);

        ChessGame.TeamColor opponent = (playerColor == ChessGame.TeamColor.WHITE)
                ? ChessGame.TeamColor.BLACK
                : ChessGame.TeamColor.WHITE;

        String opponentName = (opponent == ChessGame.TeamColor.WHITE)
                ? gameData.whiteUsername()
                : gameData.blackUsername();

        String extra = null;

        if(game.isInCheckmate(opponent)) {
            extra = opponentName + " is in checkmate";
        } else if (game.isInStalemate(opponent)) {
            extra = opponentName + " is in stalemate";
        } else if (game.isInCheck(opponent)) {
            extra = opponentName + " is in check";
        }

        return new MoveResult(game, moveText, extra);
    }

    public void leaveGame(String authToken, int gameID)
        throws ServiceException, DataAccessException {
        String username = requireAuth(authToken);
        var gameData = db.games().find(gameID);

        if (gameData == null) {
            throw new ServiceException(400, "error bad request");
        }

        String white = gameData.whiteUsername();
        String black = gameData.blackUsername();

        if (username.equals(white)) {
            white = null;
        } else if (username.equals(black)) {
            black = null;
        }

        var updated = new GameData(
                gameData.gameID(),
                white,
                black,
                gameData.gameName(),
                gameData.game()
        );
        db.games().update(updated);
    }

    public void resignGame(String authToken, int gameID)
        throws ServiceException, DataAccessException {

        String username = requireAuth(authToken);
        var gameData = db.games().find(gameID);

        if(gameData == null) {
            throw new ServiceException(400, "error bad reqeust");
        }

        boolean isWhite = username.equals(gameData.whiteUsername());
        boolean isBlack = username.equals(gameData.blackUsername());

        if (!isWhite && !isBlack) {
            throw new ServiceException(400, "observers cannot resign");
        }

        var updated = new GameData(
                gameData.gameID(),
                null,
                null,
                gameData.gameName(),
                gameData.game()
        );
        db.games().update(updated);
    }

    //notification helps

    public String buildConnectMessage(String authToken, int gameID)
        throws ServiceException, DataAccessException {

        String username = requireAuth(authToken);
        var gameData = db.games().find(gameID);
        if (gameData == null) {
            throw new ServiceException(400, "bad request");
        }

        if (username.equals(gameData.whiteUsername())) {
            return username + " connected as WHITE";
        } else if (username.equals(gameData.blackUsername())) {
            return username + " connected as BLACK";
        } else {
            return username + "connected as an observer";
        }
    }

    public String buildLeaveMessage(String authToken, int gameID)
        throws ServiceException, DataAccessException {

        String username =
    }

    //helper method
    private String requireAuth(String token)
            throws ServiceException, DataAccessException {
        AuthData a = db.auths().find(token);
        if (a == null) {
            throw new ServiceException(401, "Error: unauthorized");
        }
        return a.username();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
