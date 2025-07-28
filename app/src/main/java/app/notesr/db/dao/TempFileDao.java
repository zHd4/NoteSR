package app.notesr.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import app.notesr.model.TempFile;

import java.util.List;

@Dao
public interface TempFileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TempFile file);

    @Update
    void update(TempFile file);

    @Delete
    void delete(TempFile file);

    @Query("DELETE FROM temp_files WHERE id = :id")
    void deleteById(Long id);

    @Query("SELECT * FROM temp_files WHERE id = :id LIMIT 1")
    TempFile get(Long id);

    @Query("SELECT * FROM temp_files")
    List<TempFile> getAll();
}
