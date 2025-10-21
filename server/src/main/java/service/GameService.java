package service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import chess.ChessGame;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.GameData;


public class GameService {
    private final DataAccess db;

    public GameService(DataAccess db) {
        this.db = db;
    }

    //Post, creates a new game

    public int create(String authToken, String gameName)
        throws ServiceException {
        requireAuth(authToken);
        if (isBlank(gameName)) throw new ServiceException(400, "Error: bad request");
        try {
            var game = db.games().create(gameName);
            return game.gameID();
        } catch (DataAccessException dae) {
            throw new ServiceException(500, "Error: " + dae.getMessage());
        }
    }

    //Get game

    public Collection<GameData> list(String authToken) throws ServiceException {
        requireAuth(authToken);
        return new ArrayList<>(db.games().list());
    }

    //put game, join as white/black

    public void join(String authToken, ChessGame.TeamColor color, Integer gameID) throws ServiceException {
        var username = requireAuth(authToken);
        if (color == null || gameID == null) throw new ServiceException(400, "Error: bad request");

        var game = db.games().find(gameID);

        if (game == null) throw new ServiceException(400, "Error: bad request");

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
    //helper method
    private String requireAuth(String token)
        throws ServiceException {
        AuthData a = db.auths().find(token);
        if (a == null) throw new ServiceException(401, "Error: unauthorized");
        return a.username();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
