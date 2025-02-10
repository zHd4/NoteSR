package app.notesr.db.service;

import app.notesr.db.BaseDB;
import app.notesr.db.service.table.TempFileTable;

public class ServicesDB extends BaseDB {
    private static final String NAME = "services_db5";

    public ServicesDB() {
        super(NAME);

        tables.put(TempFileTable.class, new TempFileTable(this, "temp_files"));
    }
}
