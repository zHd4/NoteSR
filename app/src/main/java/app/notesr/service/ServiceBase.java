package app.notesr.service;

import app.notesr.App;
import app.notesr.db.notes.NotesDB;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.db.notes.table.NotesTable;

public class ServiceBase {
    protected NotesTable getNotesTable() {
        return getNotesDatabase().getTable(NotesTable.class);
    }

    protected FilesInfoTable getFilesInfoTable() {
        return getNotesDatabase().getTable(FilesInfoTable.class);
    }

    protected DataBlocksTable getDataBlocksTable() {
        return getNotesDatabase().getTable(DataBlocksTable.class);
    }

    private NotesDB getNotesDatabase() {
        return App.getAppContainer().getNotesDB();
    }
}
