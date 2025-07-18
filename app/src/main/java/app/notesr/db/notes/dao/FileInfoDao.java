package app.notesr.db.notes.dao;

import static java.util.UUID.randomUUID;

import android.content.ContentValues;
import android.database.Cursor;
import app.notesr.db.BaseDao;
import app.notesr.db.notes.NotesDb;
import app.notesr.model.EncryptedFileInfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class FileInfoDao extends BaseDao {
    public FileInfoDao(NotesDb db, String name, NoteDao noteDao) {
        super(db, name);

        db.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "("
                        + "id varchar(36) PRIMARY KEY CHECK(LENGTH(id) = 36), "
                        + "note_id varchar(36) NOT NULL, "
                        + "encrypted_name blob NOT NULL, "
                        + "encrypted_type blob, "
                        + "encrypted_thumbnail blob, "
                        + "size bigint NOT NULL, "
                        + "created_at varchar(255) NOT NULL, "
                        + "updated_at varchar(255) NOT NULL, "
                        + "FOREIGN KEY(note_id) REFERENCES " + noteDao.getName() + "(id))"
        );
    }

    public void save(EncryptedFileInfo fileInfo) {
        save(fileInfo, true);
    }

    public void save(EncryptedFileInfo fileInfo, boolean generateId) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(getTimestampFormatter());

        ContentValues values = new ContentValues();

        values.put("note_id", fileInfo.getNoteId());
        values.put("encrypted_name", fileInfo.getEncryptedName());
        values.put("encrypted_type", fileInfo.getEncryptedType());
        values.put("encrypted_thumbnail", fileInfo.getEncryptedThumbnail());
        values.put("size", fileInfo.getSize());

        if (fileInfo.getUpdatedAt() == null) {
            values.put("updated_at", nowStr);
            fileInfo.setUpdatedAt(now);
        } else {
            String updatedAt = fileInfo.getUpdatedAt().format(getTimestampFormatter());
            values.put("updated_at", updatedAt);
        }

        if (fileInfo.getId() == null || get(fileInfo.getId()) == null) {
            String id = generateId ? randomUUID().toString() : fileInfo.getId();

            values.put("id", id);
            values.put("created_at", nowStr);

            if (db.writableDatabase.insert(name, null, values) == -1) {
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

    public List<EncryptedFileInfo> getAll() {
        List<EncryptedFileInfo> filesInfo = new ArrayList<>();

        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT "
                        + "id, "
                        + "note_id, "
                        + "encrypted_name, "
                        + "encrypted_type, "
                        + "encrypted_thumbnail, "
                        + "size, "
                        + "created_at, "
                        + "updated_at "
                        + " FROM " + name
                        + " ORDER BY id",
                new String[] {});

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    EncryptedFileInfo fileInfo = EncryptedFileInfo.builder()
                            .id(cursor.getString(0))
                            .noteId(cursor.getString(1))
                            .encryptedName(cursor.getBlob(2))
                            .encryptedType(cursor.getBlob(3))
                            .encryptedThumbnail(cursor.getBlob(4))
                            .size(cursor.getLong(5))
                            .createdAt(LocalDateTime.parse(cursor.getString(6),
                                    getTimestampFormatter()))
                            .updatedAt(LocalDateTime.parse(cursor.getString(7),
                                    getTimestampFormatter()))
                            .build();

                    filesInfo.add(fileInfo);
                } while (cursor.moveToNext());
            }
        }

        return filesInfo;
    }

    public EncryptedFileInfo get(String id) {
        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT "
                        + "note_id, "
                        + "encrypted_name, "
                        + "encrypted_type, "
                        + "encrypted_thumbnail, "
                        + "size, "
                        + "created_at, "
                        + "updated_at "
                        + " FROM " + name
                        + " WHERE id = ?",
                new String[] {String.valueOf(id)});

        try (cursor) {
            if (cursor.moveToFirst()) {
                String noteId = cursor.getString(0);

                byte[] name = cursor.getBlob(1);
                byte[] type = cursor.getBlob(2);
                byte[] thumbnail = cursor.getBlob(3);

                Long size = cursor.getLong(4);

                String createdAt = cursor.getString(5);
                String updatedAt = cursor.getString(6);

                return EncryptedFileInfo.builder()
                        .id(id)
                        .noteId(noteId)
                        .size(size)
                        .encryptedName(name)
                        .encryptedType(type)
                        .encryptedThumbnail(thumbnail)
                        .createdAt(LocalDateTime.parse(createdAt, getTimestampFormatter()))
                        .updatedAt(LocalDateTime.parse(updatedAt, getTimestampFormatter()))
                        .build();
            }
        }

        return null;
    }

    public List<EncryptedFileInfo> getByNoteId(String noteId) {
        List<EncryptedFileInfo> files = new ArrayList<>();

        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT "
                        + "id, "
                        + "encrypted_name, "
                        + "encrypted_type, "
                        + "encrypted_thumbnail, "
                        + "size, "
                        + "created_at, "
                        + "updated_at"
                        + " FROM " + name
                        + " WHERE note_id = ?"
                        + " ORDER BY updated_at DESC",
                new String[] {String.valueOf(noteId)});

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(0);

                    byte[] name = cursor.getBlob(1);
                    byte[] type = cursor.getBlob(2);
                    byte[] thumbnail = cursor.getBlob(3);

                    Long size = cursor.getLong(4);

                    String createdAt = cursor.getString(5);
                    String updatedAt = cursor.getString(6);

                    EncryptedFileInfo fileInfo = EncryptedFileInfo.builder()
                            .id(id)
                            .noteId(noteId)
                            .size(size)
                            .encryptedName(name)
                            .encryptedType(type)
                            .encryptedThumbnail(thumbnail)
                            .createdAt(LocalDateTime.parse(createdAt, getTimestampFormatter()))
                            .updatedAt(LocalDateTime.parse(updatedAt, getTimestampFormatter()))
                            .build();

                    files.add(fileInfo);
                } while (cursor.moveToNext());
            }
        }

        return files;
    }

    public Long getCountByNoteId(String noteId) {
        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT COUNT(*) FROM " + name + " WHERE note_id = ?",
                new String[] {noteId});

        Long count = null;

        try (cursor) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
        }

        return count;
    }

    public void delete(String id) {
        db.writableDatabase.delete(name, "id = ?", new String[]{id});
    }
}
