package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.model.EncryptedFile;
import com.peew.notesr.model.EncryptedFileInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FilesTable extends Table {
    public FilesTable(SQLiteOpenHelper helper, String name, NotesTable notesTable) {
        super(helper, name);

        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "note_id integer NOT NULL, " +
                        "encrypted_name text NOT NULL, " +
                        "encrypted_type text, " +
                        "size bigint NOT NULL, " +
                        "encrypted_data blob NOT NULL, " +
                        "created_at varchar(255) NOT NULL, " +
                        "updated_at varchar(255) NOT NULL, " +
                        "FOREIGN KEY(note_id) REFERENCES " + notesTable.getName() + "(note_id))"
        );
    }

    public void save(EncryptedFile file) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String now = LocalDateTime.now().format(timestampFormatter);

        ContentValues values = new ContentValues();

        values.put("note_id", file.getNoteId());
        values.put("encrypted_name", file.getEncryptedName());
        values.put("encrypted_type", file.getEncryptedType());
        values.put("size", file.getSize());
        values.put("encrypted_data", file.getEncryptedData());
        values.put("updated_at", now);

        if (file.getId() == null || get(file.getId()) == null) {
            values.put("created_at", now);
            db.insert(name, null, values);
        } else {
            db.update(name, values, "id = ?",
                    new String[] {String.valueOf(file.getId())});
        }
    }

    public EncryptedFile get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " +
                        "note_id, " +
                        "encrypted_name, " +
                        "encrypted_type, " +
                        "size, " +
                        "created_at, " +
                        "updated_at, " +
                        "encrypted_data" +
                        " FROM " + name +
                        " WHERE id = ?",
                new String[] { String.valueOf(id) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                Long noteId = cursor.getLong(0);

                String name = cursor.getString(1);
                String type = cursor.getString(2);

                Long size = cursor.getLong(3);

                String createdAt = cursor.getString(4);
                String updatedAt = cursor.getString(5);

                byte[] data = cursor.getBlob(6);

                EncryptedFile file = new EncryptedFile(
                        noteId,
                        name,
                        type,
                        size,
                        LocalDateTime.parse(createdAt, timestampFormatter),
                        LocalDateTime.parse(updatedAt, timestampFormatter),
                        data);

                file.setId(id);
                return file;
            }
        }

        return null;
    }

    public List<EncryptedFileInfo> getByNoteId(long noteId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<EncryptedFileInfo> files = new ArrayList<>();

        Cursor cursor = db.rawQuery(
                "SELECT id, encrypted_name, encrypted_type, size, created_at, updated_at" +
                        " FROM " + name +
                        " WHERE note_id = ?",
                new String[] { String.valueOf(noteId) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    Long id = cursor.getLong(0);

                    String name = cursor.getString(1);
                    String type = cursor.getString(2);

                    Long size = cursor.getLong(3);

                    String createdAt = cursor.getString(4);
                    String updatedAt = cursor.getString(5);

                    files.add(new EncryptedFileInfo(
                            id,
                            noteId,
                            size,
                            name,
                            type,
                            LocalDateTime.parse(createdAt, timestampFormatter),
                            LocalDateTime.parse(updatedAt, timestampFormatter)));
                } while (cursor.moveToNext());
            }
        }

        return files;
    }

    public void delete(long id) {
        helper.getWritableDatabase()
                .delete(name, "id = ?", new String[]{ String.valueOf(id) });
    }
}
