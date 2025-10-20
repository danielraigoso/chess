package dataaccess;

import model.AuthData;

public interface AuthDAO {
    void clear();
    void insert(AuthData auth) throws DataAccessException;
    AuthData find(String authToken);
    void delete(String authToken);
}
