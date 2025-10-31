package dataaccess;

import java.sql.Connection;
import java.sql.SQLException;

public class SqlDataAccessDAO implements DataAccess{
    private final UserDAO users = new SqlUserDAO();
}
