package dataaccess;

import model.GameData;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class DataTests {

    private SqlGameDAO games;

    @BeforeAll
    static void bootstrapSchema() throws Exception {
        DatabaseManager.createDatabase();

        SqlUserDAO.createTable();
        SqlAuthDAO.createTable();
        SqlGameDAO.createTable();
    }

    
}

