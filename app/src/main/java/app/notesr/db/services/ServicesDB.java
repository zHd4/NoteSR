package app.notesr.db.services;

import app.notesr.db.BaseDB;
import app.notesr.db.services.table.TempFilesTable;

public class ServicesDB extends BaseDB {
    private static final String NAME = "services_db5";

    public ServicesDB() {
        super(NAME);

        tables.put(TempFilesTable.class, new TempFilesTable(this, "temp_files"));
    }
}
