package dataaccess;

import model.AuthData;

public class DataAccessDAO implements DataAccess {
    private final UserDataDAO users = new UserDataDAO();
    private final AuthDataDAO auths = new AuthDataDAO();
    private final GameDataDAO games = new GameDataDAO();

    @Override
    public void clearAll(){
        users.clear();
        auths.clear();
        games.clear();
    }

    @Override
    public UserDAO users() {
        return users;
    }

    @Override
    public AuthDAO auths() {
        return auths;
    }

    @Override
    public GameDAO games() {
        return games;
    }
}
