package app.notesr.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import app.notesr.model.FileBlobInfo;

@Dao
public interface FileBlobInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FileBlobInfo fileBlobInfo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FileBlobInfo> fileInfo);

    @Update
    void update(FileBlobInfo fileBlobInfo);

    @Delete
    void delete(FileBlobInfo fileBlobInfo);

    @Query("DELETE FROM files_blob_info WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM files_blob_info WHERE file_id = :fileId")
    void deleteByFileId(String fileId);

    @Query("SELECT * FROM files_blob_info ORDER BY id")
    List<FileBlobInfo> getAll();

    @Query("SELECT id FROM files_blob_info WHERE file_id = :fileId ORDER BY blob_order")
    List<String> getBlobIdsByFileId(String fileId);

    @Query("SELECT * FROM files_blob_info WHERE id = :id LIMIT 1")
    FileBlobInfo get(String id);

    @Query("SELECT COUNT(*) FROM files_blob_info")
    Long getRowsCount();
}
