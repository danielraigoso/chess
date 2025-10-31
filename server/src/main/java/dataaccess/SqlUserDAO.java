package dataaccess;

import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;


public class SqlUserDAO implements UserDAO {
    static void createTable() throws DataAccessException {
        final var sql = """
            CREATE TABLE IF NOT EXISTS users (
              username VARCHAR(64) PRIMARY KEY,
              passwordHash VARCHAR(200) NOT NULL,
              email VARCHAR(128)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating users table", e);
        }
    }


}
