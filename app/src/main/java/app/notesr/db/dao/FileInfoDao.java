package app.notesr.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import app.notesr.model.FileInfo;

@Dao
public interface FileInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FileInfo fileInfo);

    @Update
    void update(FileInfo fileInfo);

    @Delete
    void delete(FileInfo fileInfo);

    @Query("DELETE FROM files_info WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM files_info ORDER BY id")
    List<FileInfo> getAll();

    @Query("SELECT * FROM files_info WHERE id = :id LIMIT 1")
    FileInfo get(String id);

    @Query("SELECT * FROM files_info WHERE note_id = :noteId ORDER BY updated_at DESC")
    List<FileInfo> getByNoteId(String noteId);

    @Query("SELECT COUNT(*) FROM files_info WHERE note_id = :noteId")
    Long getCountByNoteId(String noteId);
}
