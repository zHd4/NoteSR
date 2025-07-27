package app.notesr.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.model.DataBlock;
import app.notesr.model.FileInfo;
import app.notesr.model.Note;

@Database(entities = {Note.class, FileInfo.class, DataBlock.class},
        version = 1,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao noteDao();
    public abstract FileInfoDao fileInfoDao();
    public abstract DataBlockDao dataBlockDao();
}

