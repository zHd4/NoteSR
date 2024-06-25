package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
                        "created_at varchar(255) NOT NULL, " +
                        "updated_at varchar(255) NOT NULL, " +
                        "FOREIGN KEY(note_id) REFERENCES " + notesTable.getName() + "(note_id))"
        );
    }

    public void save(EncryptedFileInfo fileInfo) {
        SQLiteDatabase db = helper.getWritableDatabase();

        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(timestampFormatter);

        ContentValues values = new ContentValues();

        values.put("note_id", fileInfo.getNoteId());
        values.put("encrypted_name", fileInfo.getEncryptedName());
        values.put("encrypted_type", fileInfo.getEncryptedType());
        values.put("size", fileInfo.getSize());
        values.put("updated_at", nowStr);

        if (fileInfo.getId() == null || get(fileInfo.getId()) == null) {
            values.put("created_at", nowStr);
            long id = db.insert(name, null, values);

            if (id == -1) {
                throw new RuntimeException("Cannot insert file info in table '" + name + "'");
            }

            fileInfo.setId(id);
            fileInfo.setCreatedAt(now);
        } else {
            db.update(name, values, "id = ?",
                    new String[] {String.valueOf(fileInfo.getId())});
        }

        fileInfo.setUpdatedAt(now);
    }

    public EncryptedFileInfo get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " +
                        "note_id, " +
                        "encrypted_name, " +
                        "encrypted_type, " +
                        "size, " +
                        "created_at, " +
                        "updated_at " +
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

                return new EncryptedFileInfo(
                        id,
                        noteId,
                        size,
                        name,
                        type,
                        LocalDateTime.parse(createdAt, timestampFormatter),
                        LocalDateTime.parse(updatedAt, timestampFormatter));
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
                        " WHERE note_id = ?" +
                        " ORDER BY updated_at DESC",
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
