package dataaccess;

import model.AuthData;

public class DataAccessDAO implements DataAccess {
    private final UserDataDAO users = new UserDataDAO();
    private final AuthDataDAO auths = new AuthDataDAO();
    private final GameDataDAO games = new GameDataDAO();

    @Override
    public void clearAll() {

    }
}
