package app.notesr.db.notes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import app.notesr.model.Note;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    List<Note> getAll();

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    Note get(String id);
}
