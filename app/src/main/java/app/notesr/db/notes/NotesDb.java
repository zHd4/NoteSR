package app.notesr.db.notes;

import app.notesr.db.BaseDb;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;

public class NotesDb extends BaseDb {
    private static final String NAME = "notes_db5";

    public NotesDb() {
        super(NAME);

        NoteDao noteDao = new NoteDao(this, "notes");
        FileInfoDao fileInfoDao = new FileInfoDao(this, "files_info", noteDao);
        DataBlockDao dataBlockDao = new DataBlockDao(this, "data_blocks", fileInfoDao);

        daoMap.put(NoteDao.class, noteDao);
        daoMap.put(FileInfoDao.class, fileInfoDao);
        daoMap.put(DataBlockDao.class, dataBlockDao);
    }
}
