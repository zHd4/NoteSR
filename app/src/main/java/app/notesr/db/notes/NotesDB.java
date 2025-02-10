package app.notesr.db.notes;

import app.notesr.db.BaseDB;
import app.notesr.db.notes.table.DataBlockTable;
import app.notesr.db.notes.table.FileInfoTable;
import app.notesr.db.notes.table.NoteTable;

public class NotesDB extends BaseDB {
    private static final String NAME = "notes_db5";

    public NotesDB() {
        super(NAME);

        NoteTable noteTable = new NoteTable(this, "notes");
        FileInfoTable fileInfoTable = new FileInfoTable(this, "files_info", noteTable);
        DataBlockTable dataBlockTable = new DataBlockTable(this, "data_blocks", fileInfoTable);

        tables.put(NoteTable.class, noteTable);
        tables.put(FileInfoTable.class, fileInfoTable);
        tables.put(DataBlockTable.class, dataBlockTable);
    }
}
