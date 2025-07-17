package app.notesr.db.notes;

import app.notesr.db.BaseDb;
import app.notesr.db.notes.table.DataBlockDao;
import app.notesr.db.notes.table.FileInfoDao;
import app.notesr.db.notes.table.NoteDao;

public class NotesDb extends BaseDb {
    private static final String NAME = "notes_db5";

    public NotesDb() {
        super(NAME);

        NoteDao noteTable = new NoteDao(this, "notes");
        FileInfoDao fileInfoTable = new FileInfoDao(this, "files_info", noteTable);
        DataBlockDao dataBlockTable = new DataBlockDao(this, "data_blocks", fileInfoTable);

        tables.put(NoteDao.class, noteTable);
        tables.put(FileInfoDao.class, fileInfoTable);
        tables.put(DataBlockDao.class, dataBlockTable);
    }
}
