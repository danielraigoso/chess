package dataaccess;

import java.util.concurrent.ConcurrentHashMap;
import model.AuthData;

import javax.xml.crypto.Data;

public class AuthDataDAO implements AuthDAO {
    private final ConcurrentHashMap<String, AuthData> auths = new ConcurrentHashMap<>();

    @Override public void clear() {auths.clear(); }

    @Override
    public void insert(AuthData auth) throws DataAccessException {
        if (auth == null || auth.authToken() == null || auth.username() == null) {
            throw new DataAccessException("auth fields null");
        }
        var prior = auths.putIfAbsent(auth.authToken(), auth);
        if (prior != null) throw new DataAccessException("duplicate auth token");
    }

    @Override
    public AuthData find(String authToken) {
        return auths.get(authToken);
    }

    @Override
    public void delete(String authToken) {
        if (authToken != null) {
            auths.remove(authToken);
        }
    }
}
