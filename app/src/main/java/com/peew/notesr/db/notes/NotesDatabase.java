package com.peew.notesr.db.notes;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.Table;

import java.util.Map;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final NotesDatabase INSTANCE = new NotesDatabase();
    private static final int DATABASE_VERSION = 1;
    private static final String NAME = "notes";
    private Map<Class<? extends Table>, Table> tables;

    public static NotesDatabase getInstance() {
        return INSTANCE;
    }

    private NotesDatabase() {
        super(App.getContext(), NAME, null, DATABASE_VERSION);
        onCreate(getWritableDatabase());
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        tables = Map.of(NotesTable.class, new NotesTable(this, "notes"));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public NotesTable getNotesTable() {
        return (NotesTable) tables.get(NotesTable.class);
    }
}
