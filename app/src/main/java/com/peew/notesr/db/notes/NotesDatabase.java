package com.peew.notesr.db.notes;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.Table;

import java.util.HashMap;
import java.util.Map;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String NAME = "notes";
    private final Map<Class<? extends Table>, Table> tables = new HashMap<>();

    public NotesDatabase() {
        super(App.getContext(), NAME, null, DATABASE_VERSION);

        NotesTable notesTable = new NotesTable(this, "notes");

        tables.put(NotesTable.class, notesTable);
        tables.put(FilesTable.class, new FilesTable(this, "files", notesTable));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public NotesTable getNotesTable() {
        return (NotesTable) tables.get(NotesTable.class);
    }
}
