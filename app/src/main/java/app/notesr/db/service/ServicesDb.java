package app.notesr.db.service;

import app.notesr.db.BaseDb;
import app.notesr.db.service.dao.TempFileDao;

public class ServicesDb extends BaseDb {
    private static final String NAME = "services_db5";

    public ServicesDb() {
        super(NAME);

        daoMap.put(TempFileDao.class, new TempFileDao(this, "temp_files"));
    }
}
