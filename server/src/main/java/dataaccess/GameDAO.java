package dataaccess;

import java.util.Collection;
import model.GameData;

public interface GameDAO {
    void clear() throws DataAccessException;
    GameData create(String gameName) throws DataAccessException;
    GameData find(int gameID) throws DataAccessException;
    Collection<GameData> list() throws DataAccessException;
    void update(GameData game) throws DataAccessException;
}
