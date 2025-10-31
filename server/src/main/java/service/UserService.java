package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

import javax.xml.crypto.Data;
import java.util.ServiceConfigurationError;
import java.util.UUID;


public class UserService {
    private final DataAccess db;

    public UserService(DataAccess db) {
        this.db = db;
    }

    public AuthData register(UserData req) throws ServiceException {
        if (req == null || isBlank(req.username()) || isBlank(req.password()) || isBlank(req.email())) {
            throw new ServiceException(400, "Error: bad request");
        }

        try {
            if (db.users().find(req.username()) != null) {
                throw new ServiceException(403, "Error: already taken");
            }

            db.users().insert(req);

            var token = UUID.randomUUID().toString();
            var auth = new AuthData(token, req.username());
            db.auths().insert(auth);
            return auth;

        } catch (ServiceException se) {
            throw se;
        } catch (DataAccessException dae) {
            throw new ServiceException(500, "Error: " + dae.getMessage());
        }
    }

    public AuthData login(UserData req) throws ServiceException {
        if (isBlank(req.username()) || isBlank(req.password())) {
            throw new ServiceException(400, "Error: bad request");
        }
        try {
            var dbUser = db.users().find(req.username());  // returns passwordHash in .password()
            if (dbUser == null) throw new ServiceException(401, "Error: unauthorized");

            boolean ok = org.mindrot.jbcrypt.BCrypt.checkpw(req.password(), dbUser.password());
            if (!ok) throw new ServiceException(401, "Error: wrong password");

            var auth = new AuthData(java.util.UUID.randomUUID().toString(), dbUser.username());
            db.auths().insert(auth);
            return auth;
        } catch (DataAccessException e) {
            throw new ServiceException(500, "Error: " + e.getMessage());
        }
    }

    public void logout(String authToken) throws ServiceException, DataAccessException {
        var auth = db.auths().find(authToken);
        if (auth == null) {
            throw new ServiceException(401, "Error: unauthorized");
        }
        db.auths().delete(authToken);
    }

    private static boolean isBlank(String s) {
        return s  == null || s.isBlank();
    }
}


