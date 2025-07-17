package app.notesr.db.service;

import app.notesr.db.BaseDb;
import app.notesr.db.service.table.TempFileDao;

public class ServicesDb extends BaseDb {
    private static final String NAME = "services_db5";

    public ServicesDb() {
        super(NAME);

        tables.put(TempFileDao.class, new TempFileDao(this, "temp_files"));
    }
}
