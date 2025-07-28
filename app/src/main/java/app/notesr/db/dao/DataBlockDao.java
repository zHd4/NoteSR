package app.notesr.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import app.notesr.model.DataBlock;

import java.util.List;

@Dao
public interface DataBlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DataBlock block);

    @Update
    void update(DataBlock block);

    @Delete
    void delete(DataBlock block);

    @Query("DELETE FROM data_block WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM data_block WHERE file_id = :fileId")
    void deleteByFileId(String fileId);

    @Query("SELECT * FROM data_block WHERE id = :id LIMIT 1")
    DataBlock get(String id);

    @Query("SELECT id, file_id, block_order FROM data_block ORDER BY id")
    List<DataBlock> getAllWithoutData();

    @Query("SELECT id FROM data_block WHERE file_id = :fileId ORDER BY block_order")
    List<String> getBlockIdsByFileId(String fileId);
}
