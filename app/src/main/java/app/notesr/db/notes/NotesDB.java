package app.notesr.db.notes;

import app.notesr.db.BaseDB;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.db.notes.table.NotesTable;

public class NotesDB extends BaseDB {
    private static final String NAME = "notes_db5";

    public NotesDB() {
        super(NAME);

        NotesTable notesTable = new NotesTable(this, "notes");
        FilesInfoTable filesInfoTable = new FilesInfoTable(this, "files_info", notesTable);
        DataBlocksTable dataBlocksTable = new DataBlocksTable(this, "data_blocks", filesInfoTable);

        tables.put(NotesTable.class, notesTable);
        tables.put(FilesInfoTable.class, filesInfoTable);
        tables.put(DataBlocksTable.class, dataBlocksTable);
    }
}
