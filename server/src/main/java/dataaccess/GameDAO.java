package dataaccess;

import java.util.Collection;
import model.GameData;

public interface GameDAO {
    void clear();
    GameData create(String gameName) throws DataAccessException;
    GameData find(int gameID);
    Collection<GameData> list();
    void update(GameData game) throws DataAccessException;
}
