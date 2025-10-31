package dataaccess;

import model.GameData;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlDataAccessDAO implements DataAccess{
    private final UserDAO users;
    private final AuthDAO auths;
    private final GameDAO games;

    public SqlDataAccessDAO() {
        try {
            DatabaseManager.createDatabase();
            SqlUserDAO.createTable();
            SqlAuthDAO.createTable();
            SqlGameDAO.createTable();

            this.users = new SqlUserDAO();
            this.auths = new SqlAuthDAO();
            this.games = new SqlGameDAO();
        } catch (DataAccessException e) {
            throw new RuntimeException("bootstrap failed", e);
        }
    }

    @Override
    public void clearAll() throws DataAccessException {
        auths.clear();
        games.clear();
        users.clear();
    }

    @Override public UserDAO users() {
        return users;
    }
    @Override public AuthDAO auths() {
        return auths;
    }
    @Override public GameDAO games() {
        return games;
    }
}
