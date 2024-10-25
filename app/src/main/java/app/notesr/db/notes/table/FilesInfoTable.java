package app.notesr.db.notes.table;

import android.content.ContentValues;
import android.database.Cursor;
import app.notesr.db.BaseTable;
import app.notesr.db.notes.NotesDB;
import app.notesr.model.EncryptedFileInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilesInfoTable extends BaseTable {
    public FilesInfoTable(NotesDB db, String name, NotesTable notesTable) {
        super(db, name);

        db.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "note_id integer NOT NULL, " +
                        "encrypted_name blob NOT NULL, " +
                        "encrypted_type blob, " +
                        "size bigint NOT NULL, " +
                        "created_at varchar(255) NOT NULL, " +
                        "updated_at varchar(255) NOT NULL, " +
                        "FOREIGN KEY(note_id) REFERENCES " + notesTable.getName() + "(note_id))"
        );
    }

    public void save(EncryptedFileInfo fileInfo) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(getTimestampFormatter());

        ContentValues values = new ContentValues();

        values.put("note_id", fileInfo.getNoteId());
        values.put("encrypted_name", fileInfo.getEncryptedName());
        values.put("encrypted_type", fileInfo.getEncryptedType());
        values.put("size", fileInfo.getSize());
        values.put("updated_at", nowStr);

        if (fileInfo.getId() == null || get(fileInfo.getId()) == null) {
            values.put("created_at", nowStr);
            long id = db.writableDatabase.insert(name, null, values);

            if (id == -1) {
                throw new RuntimeException("Cannot insert file info in table '" + name + "'");
            }

            fileInfo.setId(id);
            fileInfo.setCreatedAt(now);
        } else {
            db.writableDatabase.update(name, values, "id = ?",
                    new String[] {String.valueOf(fileInfo.getId())});
        }

        fileInfo.setUpdatedAt(now);
    }

    public void importFileInfo(EncryptedFileInfo fileInfo) {
        ContentValues values = new ContentValues();

        values.put("id", fileInfo.getId());
        values.put("note_id", fileInfo.getNoteId());

        values.put("encrypted_name", fileInfo.getEncryptedName());
        values.put("encrypted_type", fileInfo.getEncryptedType());

        values.put("size", fileInfo.getSize());

        String createdAt = fileInfo.getCreatedAt().format(getTimestampFormatter());
        values.put("created_at", createdAt);

        String updatedAt = fileInfo.getUpdatedAt().format(getTimestampFormatter());
        values.put("updated_at", updatedAt);

        long id = db.writableDatabase.insert(name, null, values);

        if (id == -1) {
            throw new RuntimeException("Cannot insert note in table '" + name + "'");
        }

        fileInfo.setId(id);
    }

    public EncryptedFileInfo get(long id) {
        Cursor cursor = db.readableDatabase.rawQuery(
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

                byte[] name = cursor.getBlob(1);
                byte[] type = cursor.getBlob(2);

                Long size = cursor.getLong(3);

                String createdAt = cursor.getString(4);
                String updatedAt = cursor.getString(5);

                return new EncryptedFileInfo(
                        id,
                        noteId,
                        size,
                        name,
                        type,
                        LocalDateTime.parse(createdAt, getTimestampFormatter()),
                        LocalDateTime.parse(updatedAt, getTimestampFormatter()));
            }
        }

        return null;
    }

    public List<EncryptedFileInfo> getByNoteId(long noteId) {
        List<EncryptedFileInfo> files = new ArrayList<>();

        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT id, encrypted_name, encrypted_type, size, created_at, updated_at" +
                        " FROM " + name +
                        " WHERE note_id = ?" +
                        " ORDER BY updated_at DESC",
                new String[] { String.valueOf(noteId) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    Long id = cursor.getLong(0);

                    byte[] name = cursor.getBlob(1);
                    byte[] type = cursor.getBlob(2);

                    Long size = cursor.getLong(3);

                    String createdAt = cursor.getString(4);
                    String updatedAt = cursor.getString(5);

                    files.add(new EncryptedFileInfo(
                            id,
                            noteId,
                            size,
                            name,
                            type,
                            LocalDateTime.parse(createdAt, getTimestampFormatter()),
                            LocalDateTime.parse(updatedAt, getTimestampFormatter())));
                } while (cursor.moveToNext());
            }
        }

        return files;
    }

    public Long getCountByNoteId(long noteId) {
        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM " + name + " WHERE note_id = ?",
                new String[] { String.valueOf(noteId) });

        Long count = null;

        try (cursor) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
        }

        return count;
    }

    public Set<Long> getAllIds() {
        Set<Long> ids = new HashSet<>();

        Cursor cursor = db.readableDatabase.rawQuery("SELECT id FROM " + name + " ORDER BY id", new String[] { });

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    ids.add(cursor.getLong(0));
                } while (cursor.moveToNext());
            }
        }

        return ids;
    }

    public void delete(long id) {
        db.writableDatabase.delete(name, "id = ?", new String[]{ String.valueOf(id) });
    }
}
