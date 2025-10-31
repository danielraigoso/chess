package dataaccess;

import model.GameData;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SqlGameDAO implements GameDAO{

    static void createTable() throws DataAccessException {
        final var sql = """
            CREATE TABLE IF NOT EXISTS games (
              id INT AUTO_INCREMENT PRIMARY KEY,
              name VARCHAR(128) NOT NULL,
              whiteUsername VARCHAR(64),
              blackUsername VARCHAR(64),
              gameJson MEDIUMTEXT,
              FOREIGN KEY (whiteUsername) REFERENCES users(username) ON DELETE SET NULL,
              FOREIGN KEY (blackUsername) REFERENCES users(username) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """;

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error creating game table", e);
        }
    }

    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("TRUNCATE TABLE games")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing", e);
        }
    }

    @Override
    public GameData create(String gameName) throws DataAccessException {
        final var sql = "INSERT INTO games (name) VALUES (?)";
        try (var conn = DatabaseManager.getConnection();
            var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, gameName);
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return new GameData(id, null, null, gameName, null);
                }
            }

            throw new DataAccessException("No ID returned");
        } catch (SQLException e) {
            throw new DataAccessException("Error creating game", e);
        }
    }

    @Override
    public GameData find(int gameID) throws DataAccessException {
        final var sql = "SELECT id, name, whiteusername, blackusername, gameJson FROM games WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1,gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new GameData(
                            rs.getInt("id"),
                            rs.getString("whiteusername"),
                            rs.getString("blackusername"),
                            rs.getString("name"),
                            rs.getString("gameJson")
                    );
                }
                return null;
            } catch (SQLException e) {
                throw new DataAccessException("Error finding game", e);
            }


        }
    }
}
