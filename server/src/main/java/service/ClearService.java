package service;

import dataaccess.DataAccess;

public class ClearService {
    private final DataAccess db;
    public ClearService(DataAccess db) {this.db = db;}

    public void clear() {db.clearAll();}
}
