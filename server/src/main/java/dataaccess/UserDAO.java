package dataaccess;

import model.UserData;

import javax.xml.crypto.Data;

public interface UserDAO {
    void clear();
    void insert(UserData user) throws DataAccessException;
    UserData find(String username);
}
