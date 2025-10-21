package dataaccess;

import java.util.concurrent.ConcurrentHashMap;
import model.UserData;
import org.eclipse.jetty.server.Authentication;

public class UserDataDAO implements UserDAO {
    private final ConcurrentHashMap<String, UserData> users = new ConcurrentHashMap<>();

    @Override public void clear() { users.clear(); }

    @Override
    public void insert(UserData user) throws DataAccessException {
        if (user == null || user.username() == null || user.password() == null) {
            throw new DataAccessException("use fields null");
        }
        var prior = users.putIfAbsent(user.username(),user);
        if (prior != null) throw new DataAccessException("username taken");
    }

    @Override
    public UserData find(String username) {
        return users.get(username);
    }
}
