package com.peew.notesr.db.notes;

import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.Table;
import com.peew.notesr.tools.VersionFetcher;

import java.util.List;
import java.util.stream.Collectors;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final NotesDatabase INSTANCE = new NotesDatabase();
    private static final int DATABASE_VERSION = 1;
    private static final String NAME_FORMAT = "notes_v%s";
    private List<Table> tables;

    public static NotesDatabase getInstance() {
        return INSTANCE;
    }

    private static String fetchDatabaseName() {
        try {
            String version = VersionFetcher.fetchVersionName(App.getContext(), true);
            return String.format(NAME_FORMAT, version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("fetchDatabaseName", e.toString());
            throw new RuntimeException(e);
        }
    }

    private NotesDatabase() {
        super(App.getContext(), fetchDatabaseName(), null, DATABASE_VERSION);
        onCreate(getWritableDatabase());
    }

    public void configureTables() {
        this.tables = List.of(new NotesTable(this));
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        configureTables();

        tables.forEach(table -> {
            String queryPart = table.getFields().entrySet().stream()
                    .map(field -> field.getKey() + " " + field.getValue())
                    .collect(Collectors.joining(
                            ", ",
                            table.getName() + "(", ")"));

            database.execSQL("CREATE TABLE IF NOT EXISTS " + queryPart);
        });
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
