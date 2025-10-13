package app.notesr.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import app.notesr.data.dao.FileBlobInfoDao;
import app.notesr.data.dao.FileInfoDao;
import app.notesr.data.dao.NoteDao;
import app.notesr.data.dao.TempFileDao;
import app.notesr.model.FileBlobInfo;
import app.notesr.model.FileInfo;
import app.notesr.model.Note;
import app.notesr.model.TempFile;

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

