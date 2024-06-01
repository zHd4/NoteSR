package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.model.EncryptedFile;

public class FilesTable extends Table{
    public FilesTable(SQLiteOpenHelper helper, String name) {
        super(helper, name);
        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "encrypted_name text NOT NULL, " +
                        "encrypted_data blob NOT NULL)");
    }

    public void save(EncryptedFile file) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("encrypted_name", file.getEncryptedName());
        values.put("encrypted_data", file.getEncryptedData());

        if (file.getId() == null || get(file.getId()) == null) {
            db.insert(name, null, values);
        } else {
            db.update(name, values, "id" + "=" + file.getId(), null);
        }
    }

    public EncryptedFile get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(name,
                new String[] { "encrypted_name", "encrypted_data" },
                "id" + "=" + id,
                null,
                null,
                null,
                null);

        try (cursor) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                byte[] data = cursor.getBlob(1);

                EncryptedFile file = new EncryptedFile(name, data);

                file.setId(id);
                return file;
            }
        }

        return null;
    }
}
