package app.notesr.db.notes.table;

import static java.util.UUID.randomUUID;

import android.content.ContentValues;
import android.database.Cursor;
import app.notesr.db.BaseTable;
import app.notesr.db.notes.NotesDB;
import app.notesr.model.DataBlock;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DataBlocksTable extends BaseTable {
    public DataBlocksTable(NotesDB db, String name, FilesInfoTable filesInfoTable) {
        super(db, name);

        db.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id varchar(50) PRIMARY KEY, " +
                        "file_id varchar(50) NOT NULL, " +
                        "block_order bigint NOT NULL, " +
                        "data blob NOT NULL, " +
                        "FOREIGN KEY(file_id) REFERENCES " + filesInfoTable.getName() + "(id))"
        );
    }

    public List<DataBlock> getAllWithoutData() {
        List<DataBlock> dataBlocks = new LinkedList<>();

        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT " +
                        "id, " +
                        "file_id, " +
                        "block_order" +
                        " FROM " + name +
                        " ORDER BY id",
                new String[] {});

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    DataBlock dataBlock = DataBlock.builder()
                            .id(cursor.getString(0))
                            .fileId(cursor.getString(1))
                            .order(cursor.getLong(2))
                            .build();

                    dataBlocks.add(dataBlock);
                } while (cursor.moveToNext());
            }
        }

        return dataBlocks;
    }

    public void save(DataBlock dataBlock) {
        ContentValues values = new ContentValues();

        values.put("file_id", dataBlock.getFileId());
        values.put("block_order", dataBlock.getOrder());
        values.put("data", dataBlock.getData());

        if (dataBlock.getId() == null || get(dataBlock.getId()) == null) {
            String id = randomUUID().toString();
            values.put("id", id);

            if (db.writableDatabase.insert(name, null, values) == -1) {
                throw new RuntimeException("Cannot insert data block in table '" + name + "'");
            }

            dataBlock.setId(id);
        } else {
            db.writableDatabase.update(name, values, "id = ?",
                    new String[] {String.valueOf(dataBlock.getId())});
        }
    }

    public DataBlock get(String id) {
        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT " +
                        "file_id, " +
                        "block_order, " +
                        "data " +
                        " FROM " + name +
                        " WHERE id = ?",
                new String[] {id});

        try (cursor) {
            if (cursor.moveToFirst()) {
                String fileId = cursor.getString(0);
                Long order = cursor.getLong(1);

                byte[] data = cursor.getBlob(2);

                return new DataBlock(id, fileId, order, data);
            }
        }

        return null;
    }

    public Set<String> getBlocksIdsByFileId(String fileId) {
        Set<String> ids = new TreeSet<>();

        Cursor cursor = db.readableDatabase.rawQuery(
                "SELECT id" +
                        " FROM " + name +
                        " WHERE file_id = ?" +
                        " ORDER BY block_order",
                new String[] {fileId});

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    ids.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        }

        return ids;
    }

    public void delete(String id) {
        db.writableDatabase.delete(name, "id = ?", new String[]{id});
    }

    public void deleteByFileId(String fileId) {
        db.writableDatabase.delete(name, "file_id = ?", new String[]{fileId});
    }
}
