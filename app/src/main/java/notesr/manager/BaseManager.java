package notesr.manager;

import notesr.App;
import notesr.db.notes.NotesDB;
import notesr.db.notes.table.DataBlocksTable;
import notesr.db.notes.table.FilesInfoTable;
import notesr.db.notes.table.NotesTable;

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
