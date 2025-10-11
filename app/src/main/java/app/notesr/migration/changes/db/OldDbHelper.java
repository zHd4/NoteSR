package app.notesr.migration.changes.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OldDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "notes_db5";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OldDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public final List<Map<String, Object>> getAllNotes() {
        List<Map<String, Object>> notes = new ArrayList<>();

        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM notes", new String[0]);

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(0);
                    byte[] encryptedName = cursor.getBlob(1);
                    byte[] encryptedText = cursor.getBlob(2);
                    String updatedAtStr = cursor.getString(3);

                    LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr,
                            TIMESTAMP_FORMATTER);

                    Map<String, Object> note = Map.of(
                            "id", id,
                            "encryptedName", encryptedName,
                            "encryptedText", encryptedText,
                            "updatedAt", updatedAt
                    );

                    notes.add(note);
                } while (cursor.moveToNext());
            }
        }

        return notes;
    }

    public final List<Map<String, Object>> getFilesInfo() {
        List<Map<String, Object>> filesInfo = new ArrayList<>();

        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM files_info",
                new String[0]);

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(0);
                    String noteId = cursor.getString(1);
                    byte[] encryptedName = cursor.getBlob(2);
                    byte[] encryptedType = cursor.getBlob(3);
                    byte[] encryptedThumbnail = cursor.getBlob(4);
                    Long size = cursor.getLong(5);
                    String createdAtStr = cursor.getString(6);
                    String updatedAtStr = cursor.getString(7);

                    LocalDateTime createdAt = LocalDateTime.parse(createdAtStr,
                            TIMESTAMP_FORMATTER);

                    LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr,
                            TIMESTAMP_FORMATTER);

                    Map<String, Object> fileInfo = new HashMap<>();

                    fileInfo.put("id", id);
                    fileInfo.put("noteId", noteId);
                    fileInfo.put("encryptedName", encryptedName);
                    fileInfo.put("encryptedType", encryptedType);
                    fileInfo.put("encryptedThumbnail", encryptedThumbnail);
                    fileInfo.put("size", size);
                    fileInfo.put("createdAt", createdAt);
                    fileInfo.put("updatedAt", updatedAt);

                    filesInfo.add(fileInfo);
                } while (cursor.moveToNext());
            }
        }

        return filesInfo;
    }

    public final List<String> getBlocksIdsByFileId(String fileId) {
        List<String> dataBlocksIds = new ArrayList<>();

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id FROM data_blocks WHERE file_id = ? ORDER BY block_order",
                new String[] {fileId}
        );

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    dataBlocksIds.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        }

        return dataBlocksIds;
    }

    public final Map<String, Object> getDataBlockById(String id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT file_id, block_order, data FROM data_blocks WHERE id = ?",
                new String[] {id}
        );

        try (cursor) {
            if (cursor.moveToFirst()) {
                String fileId = cursor.getString(0);
                Long order = cursor.getLong(1);
                byte[] encryptedData = cursor.getBlob(2);

                return Map.of(
                        "id", id,
                        "fileId", fileId,
                        "order", order,
                        "encryptedData", encryptedData
                );
            }
        }

        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) { }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) { }
}
