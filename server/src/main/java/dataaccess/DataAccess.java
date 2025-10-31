package dataaccess;

public interface DataAccess {
    void clearAll() throws DataAccessException;
    UserDAO users();
    AuthDAO auths();
    GameDAO games();
}
