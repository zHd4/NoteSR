/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import app.notesr.data.dao.FileBlobInfoDao;
import app.notesr.data.dao.FileInfoDao;
import app.notesr.data.dao.NoteDao;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.data.model.Note;

@Database(entities = {Note.class, FileInfo.class, FileBlobInfo.class},
        version = 2,
        exportSchema = false)
@TypeConverters({DatabaseTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract NoteDao getNoteDao();
    public abstract FileInfoDao getFileInfoDao();
    public abstract FileBlobInfoDao getFileBlobInfoDao();
}

