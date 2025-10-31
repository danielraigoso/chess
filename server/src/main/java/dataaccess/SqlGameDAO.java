package dataaccess;

import model.GameData;

import com.google.gson.Gson;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SqlGameDAO implements GameDAO {

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
            stmt.setInt(1, gameID);
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
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding game", e);
        }
    }

    @Override
    public Collection<GameData> list () throws DataAccessException {
        var games = new ArrayList<GameData>();
        final var sql = "SELECT id, name, whiteusername, blackusername, gameJson FROM games";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                games.add(new GameData(
                        rs.getInt("id"),
                        rs.getString("whiteusername"),
                        rs.getString("blackusername"),
                        rs.getString("name"),
                        rs.getString("gameJson")
                ));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error listing games", e);
        }
        return games;
    }

    @Override
    public void update(GameData game) throws DataAccessException {
        final var sql = "UPDATE games SET name=?, whiteusername=?, blackusername=?, gameJson=? WHERE id=?";
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            Gson gson = new Gson();
            String gameJson = gson.toJson(game.game());

            stmt.setString(1, game.gameName());
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            stmt.setString(4, gameJson);
            stmt.setInt(5, game.gameID());
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DataAccessException("game not found");
            }
        } catch (SQLException e) {
            throw new DataAccessException("error updating game", e);
        }
    }
}

