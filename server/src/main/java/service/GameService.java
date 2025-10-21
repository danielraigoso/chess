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
