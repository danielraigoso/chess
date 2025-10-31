package dataaccess;

import model.AuthData;

import java.sql.*;

public class SqlAuthDAO implements AuthDAO {

    static void createTable() throws DataAccessException {
        final var sql = """
            CREATE TABLE IF NOT EXISTS auths (
              authToken VARCHAR(128) PRIMARY KEY,
              username VARCHAR(64) NOT NULL,
              FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating auths table", e);
        }
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM auths")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing auths", e);
        }
    }


    @Override
    public void insert(AuthData auth) throws DataAccessException {
        if (auth == null || auth.authToken() == null || auth.username() == null) {
            throw new DataAccessException("auth fields null");
        }
        final var sql = "INSERT INTO auths (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new DataAccessException("duplicate auth token", e);
            }
            throw new DataAccessException("Error inserting auth", e);
        }
    }

    @Override
    public AuthData find(String authToken) throws DataAccessException {
        final var sql = "SELECT authToken, username FROM auths WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(rs.getString("authToken"), rs.getString("username"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding auth", e);
        }
    }

    @Override
    public void delete(String authToken) throws DataAccessException {
        final var sql = "DELETE FROM auths WHERE authToken = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, authToken);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting auth", e);
        }
    }

}
