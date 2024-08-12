package com.peew.notesr.db.notes.table;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import com.peew.notesr.db.BaseDB;
import com.peew.notesr.db.BaseTable;
import com.peew.notesr.model.DataBlock;

import java.util.Set;
import java.util.TreeSet;

public class DataBlocksTable extends BaseTable {
    public DataBlocksTable(SQLiteOpenHelper helper,
                           BaseDB.Databases databases,
                           String name,
                           FilesInfoTable filesInfoTable) {
        super(helper, databases, name);

        databases.writable.execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "file_id integer NOT NULL, " +
                        "block_order bigint NOT NULL, " +
                        "data blob NOT NULL, " +
                        "FOREIGN KEY(file_id) REFERENCES " + filesInfoTable.getName() + "(id))"
        );
    }

    public void save(DataBlock dataBlock) {
        ContentValues values = new ContentValues();

        values.put("file_id", dataBlock.getFileId());
        values.put("block_order", dataBlock.getOrder());
        values.put("data", dataBlock.getData());

        if (dataBlock.getId() == null || get(dataBlock.getId()) == null) {
            long id = databases.writable.insert(name, null, values);

            if (id == -1) {
                throw new RuntimeException("Cannot insert data block in table '" + name + "'");
            }

            dataBlock.setId(id);
        } else {
            databases.writable.update(name, values, "id = ?",
                    new String[] {String.valueOf(dataBlock.getId())});
        }
    }

    public void importDataBlock(DataBlock dataBlock) {
        ContentValues values = new ContentValues();

        values.put("id", dataBlock.getId());
        values.put("file_id", dataBlock.getFileId());

        values.put("block_order", dataBlock.getOrder());
        values.put("data", dataBlock.getData());

        long id = databases.writable.insert(name, null, values);

        if (id == -1) {
            throw new RuntimeException("Cannot insert note in table '" + name + "'");
        }

        dataBlock.setId(id);
    }

    public DataBlock get(long id) {
        Cursor cursor = databases.readable.rawQuery(
                "SELECT " +
                        "file_id, " +
                        "block_order, " +
                        "data " +
                        " FROM " + name +
                        " WHERE id = ?",
                new String[] { String.valueOf(id) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                Long fileId = cursor.getLong(0);
                Long order = cursor.getLong(1);

                byte[] data = cursor.getBlob(2);

                return new DataBlock(id, fileId, order, data);
            }
        }

        return null;
    }

    public Set<Long> getBlocksIdsByFileId(long fileId) {
        Set<Long> ids = new TreeSet<>();

        Cursor cursor = databases.readable.rawQuery(
                "SELECT id" +
                        " FROM " + name +
                        " WHERE file_id = ?" +
                        " ORDER BY block_order",
                new String[] { String.valueOf(fileId) });

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
        databases.writable.delete(name, "id = ?", new String[]{ String.valueOf(id) });
    }

    public void deleteByFileId(long fileId) {
        databases.writable.delete(name, "file_id = ?", new String[]{ String.valueOf(fileId) });
    }
}
