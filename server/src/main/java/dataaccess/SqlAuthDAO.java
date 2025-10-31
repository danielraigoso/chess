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
}
