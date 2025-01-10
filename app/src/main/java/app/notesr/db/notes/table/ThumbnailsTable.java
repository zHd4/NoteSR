package app.notesr.db.notes.table;

import static java.util.UUID.randomUUID;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.LinkedHashSet;
import java.util.Set;

import app.notesr.db.BaseTable;
import app.notesr.db.notes.NotesDB;
import app.notesr.model.EncryptedFileThumbnail;

public class ThumbnailsTable extends BaseTable {
    public ThumbnailsTable(NotesDB db, String name, FilesInfoTable filesInfoTable) {
        super(db, name);

        db.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id varchar(36) PRIMARY KEY CHECK(LENGTH(id) = 36), " +
                        "file_id varchar(36) NOT NULL, " +
                        "encrypted_image blob NOT NULL, " +
                        "FOREIGN KEY(file_id) REFERENCES " + filesInfoTable.getName() + "(id))"
        );
    }

    public void save(EncryptedFileThumbnail thumbnail) {
        save(thumbnail, true);
    }

    public void save(EncryptedFileThumbnail thumbnail, boolean generateId) {
        ContentValues values = new ContentValues();

        values.put("file_id", thumbnail.getFileId());
        values.put("encrypted_image", thumbnail.getEncryptedImage());

        if (thumbnail.getId() == null || get(thumbnail.getId()) == null) {
            String id = generateId ? randomUUID().toString() : thumbnail.getId();
            values.put("id", id);

            if (db.writableDatabase.insert(name, null, values) == -1) {
                throw new RuntimeException("Cannot insert thumbnail in table '" + name + "'");
            }

            thumbnail.setId(id);
        } else {
            db.writableDatabase.update(name, values, "id = ?",
                    new String[] {String.valueOf(thumbnail.getId())});
        }
    }

    public EncryptedFileThumbnail get(String id) {
        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT " +
                        "file_id, " +
                        "encrypted_image " +
                        " FROM " + name +
                        " WHERE id = ?",
                new String[] {id});

        try (cursor) {
            if (cursor.moveToFirst()) {
                String fileId = cursor.getString(0);
                byte[] encryptedImage = cursor.getBlob(1);

                return EncryptedFileThumbnail.builder()
                        .id(id)
                        .fileId(fileId)
                        .encryptedImage(encryptedImage)
                        .build();
            }
        }

        return null;
    }

    public Set<EncryptedFileThumbnail> getByFileId(String fileId) {
        Set<EncryptedFileThumbnail> thumbnails = new LinkedHashSet<>();

        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT " +
                        "id, " +
                        "encrypted_image " +
                        " FROM " + name +
                        " WHERE file_id = ?",
                new String[] {fileId});

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(0);
                    byte[] encryptedImage = cursor.getBlob(1);

                    EncryptedFileThumbnail thumbnail = EncryptedFileThumbnail.builder()
                            .id(id)
                            .fileId(fileId)
                            .encryptedImage(encryptedImage)
                            .build();

                    thumbnails.add(thumbnail);
                } while (cursor.moveToNext());
            }
        }

        return thumbnails;
    }
}
