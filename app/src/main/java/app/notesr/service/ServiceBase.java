package app.notesr.service;

import app.notesr.App;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;

public class ServiceBase {
    protected NoteDao getNoteTable() {
        return getNotesDatabase().getTable(NoteDao.class);
    }

    protected FileInfoDao getFileInfoTable() {
        return getNotesDatabase().getTable(FileInfoDao.class);
    }

    protected DataBlockDao getDataBlockTable() {
        return getNotesDatabase().getTable(DataBlockDao.class);
    }

    private NotesDb getNotesDatabase() {
        return App.getAppContainer().getNotesDB();
    }
}
