package com.peew.notesr.db.notes;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.Table;

import java.util.HashMap;
import java.util.Map;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String NAME = "notes_db5";
    private final Map<Class<? extends Table>, Table> tables = new HashMap<>();

    public NotesDatabase() {
        super(App.getContext(), NAME, null, DATABASE_VERSION);

        NotesTable notesTable = new NotesTable(this, "notes");
        FilesTable filesTable = new FilesTable(this, "files_info", notesTable);
        DataBlocksTable dataBlocksTable = new DataBlocksTable(this, "data_blocks", filesTable);

        tables.put(NotesTable.class, notesTable);
        tables.put(FilesTable.class, filesTable);
        tables.put(DataBlocksTable.class, dataBlocksTable);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public <T extends Table> T getTable(Class<? extends Table> tableClass) {
        return (T) tables.get(tableClass);
    }
}
