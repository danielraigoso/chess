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
    }
}
