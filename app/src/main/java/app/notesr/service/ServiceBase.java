package app.notesr.service;

import app.notesr.App;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.notes.table.DataBlockTable;
import app.notesr.db.notes.table.FileInfoTable;
import app.notesr.db.notes.table.NoteTable;

public class ServiceBase {
    protected NoteTable getNoteTable() {
        return getNotesDatabase().getTable(NoteTable.class);
    }

    protected FileInfoTable getFileInfoTable() {
        return getNotesDatabase().getTable(FileInfoTable.class);
    }

    protected DataBlockTable getDataBlockTable() {
        return getNotesDatabase().getTable(DataBlockTable.class);
    }

    private NotesDB getNotesDatabase() {
        return App.getAppContainer().getNotesDB();
    }
}
