package dataaccess;

public interface DataAccess {
    void clearAll();
    UserDAO users();
    AuthDAO auths();
    GameDAO games();
}
