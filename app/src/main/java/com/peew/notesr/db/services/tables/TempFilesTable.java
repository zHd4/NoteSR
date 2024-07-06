package com.peew.notesr.db.services.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import com.peew.notesr.db.BaseTable;
import com.peew.notesr.model.TempFile;

import java.util.ArrayList;
import java.util.List;

public class TempFilesTable extends BaseTable {
    public TempFilesTable(SQLiteOpenHelper helper, String name) {
        super(helper, name);

        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "uri text NOT NULL UNIQUE)"
        );
    }

    public void save(TempFile tempFile) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("uri", tempFile.getUri().toString());

        if (tempFile.getId() == null || get(tempFile.getId()) == null) {
            long id = db.insert(getName(), null, values);

            if (id == -1) {
                throw new RuntimeException("Cannot insert temp file in table '" + getName() + "'");
            }

            tempFile.setId(id);
        } else {
            db.update(getName(), values, "id = ?", new String[] { tempFile.getId().toString() });
        }
    }

    public TempFile get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT uri" +
                " FROM " + getName() +
                " WHERE id = ?",
                new String[] { String.valueOf(id) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                Uri uri = Uri.parse(cursor.getString(0));
                return new TempFile(id, uri);
            }
        }

        return null;
    }

    public List<TempFile> getAll() {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + getName(), null);
        List<TempFile> files = new ArrayList<>();

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);
                    Uri uri = Uri.parse(cursor.getString(1));

                    files.add(new TempFile(id, uri));
                } while (cursor.moveToNext());
            }
        }

        return files;
    }

    public void delete(Long id) {
        helper.getWritableDatabase()
                .delete(getName(), "id = ?", new String[] { id.toString() });
    }
}
