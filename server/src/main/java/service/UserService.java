package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

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

    public AuthData
}
