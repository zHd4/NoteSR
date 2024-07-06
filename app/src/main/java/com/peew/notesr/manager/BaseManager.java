package com.peew.notesr.manager;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.NotesDB;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesInfoTable;
import com.peew.notesr.db.notes.tables.NotesTable;

public class BaseManager {
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
