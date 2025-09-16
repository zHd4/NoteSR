package app.notesr.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import app.notesr.db.dao.FileBlobInfoDao;
import app.notesr.db.dao.FileInfoDao;
import app.notesr.db.dao.NoteDao;
import app.notesr.db.dao.TempFileDao;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.note.model.Note;
import app.notesr.cleaner.model.TempFile;

@Database(entities = {Note.class, FileInfo.class, FileBlobInfo.class, TempFile.class},
        version = 1,
        exportSchema = false)
@TypeConverters({DatabaseTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao getNoteDao();
    public abstract FileInfoDao getFileInfoDao();
    public abstract FileBlobInfoDao getFileBlobInfoDao();
    public abstract TempFileDao getTempFileDao();
}

