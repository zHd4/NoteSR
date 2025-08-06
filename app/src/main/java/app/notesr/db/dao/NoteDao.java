package app.notesr.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.time.LocalDateTime;
import java.util.List;

import app.notesr.model.Note;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("UPDATE notes SET updated_at = :updatedAt WHERE id = :id")
    void setUpdatedAtById(String id, LocalDateTime updatedAt);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    List<Note> getAll();

    @Query("SELECT * FROM notes WHERE id = :id")
    Note get(String id);

    @Query("""
                SELECT id FROM notes
                WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
                   OR text LIKE '%' || :query || '%' COLLATE NOCASE
            """)
    List<String> search(String query);

    @Query("SELECT COUNT(*) FROM notes")
    Long getRowsCount();
}
