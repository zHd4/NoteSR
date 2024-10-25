package app.notesr.db.notes.table;

import android.content.ContentValues;
import android.database.Cursor;
import app.notesr.db.BaseTable;
import app.notesr.db.notes.NotesDB;
import app.notesr.model.EncryptedNote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class NotesTable extends BaseTable {
    public NotesTable(NotesDB db, String name) {
        super(db, name);

        db.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                "note_id integer PRIMARY KEY AUTOINCREMENT, " +
                "encrypted_name blob NOT NULL, " +
                "encrypted_data blob NOT NULL, " +
                "updated_at varchar(255) NOT NULL)"
        );
    }

    public void save(EncryptedNote note) {
        ContentValues values = new ContentValues();

        values.put("encrypted_name", note.getEncryptedName());
        values.put("encrypted_data", note.getEncryptedText());

        if (note.getUpdatedAt() == null) {
            LocalDateTime now = LocalDateTime.now();
            String nowStr = now.format(getTimestampFormatter());

            values.put("updated_at", nowStr);
            note.setUpdatedAt(now);
        } else {
            String updatedAt = note.getUpdatedAt().format(getTimestampFormatter());
            values.put("updated_at", updatedAt);
        }

        if (note.getId() == null || get(note.getId()) == null) {
            long id = db.writableDatabase.insert(name, null, values);

            if (id == -1) {
                throw new RuntimeException("Cannot insert note in table '" + name + "'");
            }

            note.setId(id);
        } else {
            db.writableDatabase.update(name, values, "note_id = ?" ,
                    new String[] { String.valueOf(note.getId()) });
        }
    }

    public void importNote(EncryptedNote note) {
        ContentValues values = new ContentValues();

        values.put("note_id", note.getId());

        values.put("encrypted_name", note.getEncryptedName());
        values.put("encrypted_data", note.getEncryptedText());

        String updatedAt = note.getUpdatedAt().format(getTimestampFormatter());
        values.put("updated_at", updatedAt);

        long id = db.writableDatabase.insert(name, null, values);

        if (id == -1) {
            throw new RuntimeException("Cannot insert note in table '" + name + "'");
        }

        note.setId(id);
    }

    public List<EncryptedNote> getAll() {
        List<EncryptedNote> notes = new ArrayList<>();

        Cursor cursor = db.readableDatabase.rawQuery("SELECT * FROM " + name + " ORDER BY updated_at DESC",
                new String[0]);

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);

                    byte[] name = cursor.getBlob(1);
                    byte[] text = cursor.getBlob(2);

                    String updatedAtStr = cursor.getString(3);
                    LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr, getTimestampFormatter());

                    EncryptedNote note = new EncryptedNote(name, text);

                    note.setId(id);
                    note.setUpdatedAt(updatedAt);

                    notes.add(note);
                } while (cursor.moveToNext());
            }
        }

        return notes;
    }

    public EncryptedNote get(long id) {
        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT encrypted_name, encrypted_data, updated_at" +
                        " FROM " + name + " WHERE note_id = ?",
                new String[] { String.valueOf(id) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                byte[] name = cursor.getBlob(0);
                byte[] text = cursor.getBlob(1);

                String updatedAtStr = cursor.getString(2);
                LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr, getTimestampFormatter());

                EncryptedNote note = new EncryptedNote(name, text);

                note.setId(id);
                note.setUpdatedAt(updatedAt);

                return note;
            }
        }

        return null;
    }

    public void delete(long id) {
        db.writableDatabase.delete(name, "note_id" + "=" + id, null);
    }
}
