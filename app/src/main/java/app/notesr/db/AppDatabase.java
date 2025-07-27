package app.notesr.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.db.service.dao.TempFileDao;
import app.notesr.model.DataBlock;
import app.notesr.model.FileInfo;
import app.notesr.model.Note;
import app.notesr.model.TempFile;

@Database(entities = {Note.class, FileInfo.class, DataBlock.class, TempFile.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao getNoteDao();
    public abstract FileInfoDao getFileInfoDao();
    public abstract DataBlockDao getDataBlockDao();
    public abstract TempFileDao getTempFileDao();
}

