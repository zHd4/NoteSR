package com.peew.notesr.db.notes;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.Table;

import java.util.Map;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String NAME = "notes";
    private final Map<Class<? extends Table>, Table> tables;

    public NotesDatabase() {
        super(App.getContext(), NAME, null, DATABASE_VERSION);

        tables = Map.of(
                NotesTable.class, new NotesTable(this, "notes"),
                FilesTable.class, new FilesTable(this, "files")
        );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public NotesTable getNotesTable() {
        return (NotesTable) tables.get(NotesTable.class);
    }
}
