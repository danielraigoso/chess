package dataaccess;

import model.UserData;


public interface UserDAO {
    void clear() throws DataAccessException;
    void insert(UserData user) throws DataAccessException;
    UserData find(String username) throws DataAccessException;
}
