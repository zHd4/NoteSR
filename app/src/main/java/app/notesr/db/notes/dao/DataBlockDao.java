package app.notesr.db.notes.dao;

import static java.util.UUID.randomUUID;

import android.content.ContentValues;
import android.database.Cursor;
import app.notesr.db.BaseDao;
import app.notesr.db.notes.NotesDb;
import app.notesr.model.DataBlock;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DataBlockDao extends BaseDao {
    public DataBlockDao(NotesDb db, String name, FileInfoDao fileInfoDao) {
        super(db, name);

        db.writableDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id varchar(36) PRIMARY KEY CHECK(LENGTH(id) = 36), " +
                        "file_id varchar(36) NOT NULL, " +
                        "block_order bigint NOT NULL, " +
                        "data blob NOT NULL, " +
                        "FOREIGN KEY(file_id) REFERENCES " + fileInfoDao.getName() + "(id))"
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
        save(dataBlock, true);
    }

    public void save(DataBlock dataBlock, boolean generateId) {
        ContentValues values = new ContentValues();

        values.put("file_id", dataBlock.getFileId());
        values.put("block_order", dataBlock.getOrder());
        values.put("data", dataBlock.getData());

        if (dataBlock.getId() == null || get(dataBlock.getId()) == null) {
            String id = generateId ? randomUUID().toString() : dataBlock.getId();
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
        Set<String> ids = new LinkedHashSet<>();

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
