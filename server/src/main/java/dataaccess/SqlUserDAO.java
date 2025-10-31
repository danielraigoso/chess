package dataaccess;

import model.UserData;
import org.eclipse.jetty.server.Authentication;
import org.mindrot.jbcrypt.BCrypt;

import javax.xml.crypto.Data;
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


    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("DELETE FROM users")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing users", e);
        }
    }

    @Override
    public void insert(UserData user) throws DataAccessException {
        if (user == null || user.username() == null || user.password() == null) {
            throw new DataAccessException("fields null");
        }

        final var sql = "INSERT INTO users (username, passwordHash, email) VALUES (?, ?, ?)";
        String hash = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.username());
            stmt.setString(2, hash);
            stmt.setString(3, user.email());
            stmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new DataAccessException("username taken", e);
            }
            throw new DataAccessException("error inserting user", e);
        }
    }

    @Override
    public UserData find(String username) throws DataAccessException {
        final var sql = "SELECT username, passwordHash, email FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Note: we return the HASH in password() field for the service to verify with BCrypt
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("passwordHash"),
                            rs.getString("email"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding user", e);
        }
    }
}
