package dataaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import model.GameData;


public class GameDataDAO implements GameDAO{
    private final ConcurrentHashMap<Integer, GameData> games = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public void clear(){
        games.clear();
        nextId.set(1);
    }

    @Override
    public GameData create(String gameName) throws DataAccessException {
        if (gameName == null || gameName.isBlank()) {
            throw new DataAccessException("game name required");
        }

        int id = nextId.getAndIncrement();
        var game = new GameData(id, null,null,gameName, null);
        games.put(id,game);
        return game;
    }

    @Override
    public GameData find(int gameID) {
        return games.get(gameID);
    }

    @Override
    public Collection<GameData> list() {
        return new ArrayList<>(games.values());
    }

    @Override
    public void update(GameData game) throws DataAccessException {
        if (game == null) {
            throw new DataAccessException("null game");
        }

        int id = game.gameID();
        if (!games.containsKey(id)) {
            throw new DataAccessException("game not found: " + id);
        }

        games.put(id,game);
    }
}
