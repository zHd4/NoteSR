package com.peew.notesr.db.notes;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.Table;
import com.peew.notesr.db.notes.tables.TableName;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final NotesDatabase INSTANCE = new NotesDatabase();
    private static final int DATABASE_VERSION = 1;
    private static final String NAME = "notes";
    private Map<TableName, Table> tables;

    public static NotesDatabase getInstance() {
        return INSTANCE;
    }

    private NotesDatabase() {
        super(App.getContext(), NAME, null, DATABASE_VERSION);
        onCreate(getWritableDatabase());
    }

    public void configureTables() {
        this.tables = Map.of(TableName.NOTES_TABLE, new NotesTable(this));
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        configureTables();

        tables.forEach((key, table) -> {
            String name = table.getName();

            Set<Map.Entry<String, String>> fields = table.getFields().entrySet();
            String signature = fields.stream()
                    .map(field -> field.getKey() + " " + field.getValue())
                    .collect(Collectors.joining(", ", name + "(", ")"));

            database.execSQL("CREATE TABLE IF NOT EXISTS " + signature);
        });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Deprecated
    public Table getTable(TableName name) {
        return tables.get(name);
    }

    public NotesTable getNotesTable() {
        return (NotesTable) tables.get(TableName.NOTES_TABLE);
    }
}
