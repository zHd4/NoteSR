package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.model.EncryptedFile;

import java.util.ArrayList;
import java.util.List;

public class FilesTable extends Table{
    public FilesTable(SQLiteOpenHelper helper, String name, NotesTable notesTable) {
        super(helper, name);
        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "note_id integer NOT NULL, " +
                        "encrypted_name text NOT NULL, " +
                        "encrypted_type text, " +
                        "encrypted_data blob NOT NULL, " +
                        "FOREIGN KEY(note_id) REFERENCES " + notesTable.getName() + "(note_id))");
    }

    public void save(EncryptedFile file) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("note_id", file.getNoteId());
        values.put("encrypted_name", file.getEncryptedName());
        values.put("encrypted_type", file.getEncryptedType());
        values.put("encrypted_data", file.getEncryptedData());

        if (file.getId() == null || get(file.getId()) == null) {
            db.insert(name, null, values);
        } else {
            db.update(name, values, "id" + "=" + file.getId(), null);
        }
    }

    public EncryptedFile get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT note_id, encrypted_name, encrypted_type, encrypted_data" +
                        " FROM " + name +
                        " WHERE id = ?",
                new String[] { String.valueOf(id) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                Long noteId = cursor.getLong(0);

                String name = cursor.getString(1);
                String type = cursor.getString(2);

                byte[] data = cursor.getBlob(3);
                EncryptedFile file = new EncryptedFile(noteId, name, type, data);

                file.setId(id);
                return file;
            }
        }

        return null;
    }

    public List<EncryptedFile> getByNoteId(long noteId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<EncryptedFile> files = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT id, encrypted_name, encrypted_type, encrypted_data" +
                        " FROM " + name +
                        " WHERE note_id = ?",
                new String[] { String.valueOf(noteId) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    Long id = cursor.getLong(0);

                    String name = cursor.getString(1);
                    String type = cursor.getString(2);

                    byte[] data = cursor.getBlob(3);
                    EncryptedFile file = new EncryptedFile(noteId, name, type, data);

                    file.setId(id);
                    files.add(file);
                } while (cursor.moveToNext());
            }
        }

        return files;
    }

    public void delete(long id) {
        helper.getWritableDatabase()
                .delete(name, "id" + "=" + id, null);
    }
}
