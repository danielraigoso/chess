package dataaccess;

import model.AuthData;

public interface AuthDAO {
    void clear() throws DataAccessException;
    void insert(AuthData auth) throws DataAccessException;
    AuthData find(String authToken) throws DataAccessException;
    void delete(String authToken) throws DataAccessException;
}
