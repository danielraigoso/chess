package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.GameData;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

public class SqlGameDAO implements GameDAO {

    private static final Gson GSON = new Gson();

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
        // TRUNCATE may fail with FKs; DELETE works everywhere, then reset AUTO_INCREMENT (optional)
        try (var conn = DatabaseManager.getConnection()) {
            try (var delete = conn.prepareStatement("DELETE FROM games")) {
                delete.executeUpdate();
            }
            try (var reset = conn.prepareStatement("ALTER TABLE games AUTO_INCREMENT = 1")) {
                reset.executeUpdate();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error clearing games", e);
        }
    }

    @Override
    public GameData create(String gameName) throws DataAccessException {
        if (gameName == null || gameName.isBlank()) {
            throw new DataAccessException("game name required");
        }

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
            throw new DataAccessException("No generated id returned");
        } catch (SQLException e) {
            throw new DataAccessException("Error creating game", e);
        }
    }

    @Override
    public GameData find(int gameID) throws DataAccessException {
        final var sql = """
            SELECT id, name, whiteUsername, blackUsername, gameJson
            FROM games WHERE id = ?
        """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameID);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding game", e);
        }
    }

    @Override
    public Collection<GameData> list() throws DataAccessException {
        final var sql = "SELECT id, name, whiteUsername, blackUsername, gameJson FROM games";
        var games = new ArrayList<GameData>();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                games.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error listing games", e);
        }
        return games;
    }

    @Override
    public void update(GameData game) throws DataAccessException {
        final var sql = """
            UPDATE games
            SET name = ?, whiteUsername = ?, blackUsername = ?, gameJson = ?
            WHERE id = ?
        """;
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            // serialize ChessGame -> JSON (allow null)
            String json = (game.game() == null) ? null : GSON.toJson(game.game());

            stmt.setString(1, game.gameName());
            stmt.setString(2, game.whiteUsername());
            stmt.setString(3, game.blackUsername());
            if (json == null) stmt.setNull(4, Types.LONGVARCHAR); else stmt.setString(4, json);
            stmt.setInt(5, game.gameID());

            int updated = stmt.executeUpdate();
            if (updated == 0) {
                throw new DataAccessException("game not found: " + game.gameID());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error updating game", e);
        }
    }

    private GameData mapRow(ResultSet rs) throws SQLException {
        String json = rs.getString("gameJson"); // may be null
        ChessGame game = (json == null) ? null : GSON.fromJson(json, ChessGame.class);
        return new GameData(
                rs.getInt("id"),
                rs.getString("whiteUsername"),
                rs.getString("blackUsername"),
                rs.getString("name"),
                game
        );
    }
}
