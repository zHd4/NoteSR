package app.notesr.db.notes;

import app.notesr.db.BaseDB;
import app.notesr.db.notes.table.DataBlockTable;
import app.notesr.db.notes.table.FileInfoTable;
import app.notesr.db.notes.table.NotesTable;

public class NotesDB extends BaseDB {
    private static final String NAME = "notes_db5";

    public NotesDB() {
        super(NAME);

        NotesTable notesTable = new NotesTable(this, "notes");
        FileInfoTable fileInfoTable = new FileInfoTable(this, "files_info", notesTable);
        DataBlockTable dataBlockTable = new DataBlockTable(this, "data_blocks", fileInfoTable);

        tables.put(NotesTable.class, notesTable);
        tables.put(FileInfoTable.class, fileInfoTable);
        tables.put(DataBlockTable.class, dataBlockTable);
    }
}
