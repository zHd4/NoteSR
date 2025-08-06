package app.notesr.service.note;

import app.notesr.db.AppDatabase;
import app.notesr.model.Note;
import app.notesr.util.HashHelper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NoteService {
    private final AppDatabase db;

    public void save(Note note) {
        if (note.getId() == null) {
            note.setId(UUID.randomUUID().toString());
        }

        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }

        db.getNoteDao().insert(note);
    }

    public List<Note> getAll() {
        return db.getNoteDao()
                .getAll()
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public Note get(String id) {
        Note note = db.getNoteDao().get(id);

        if (note != null) {
            setDecimalId(note);
            return note;
        }

        return null;
    }

    public List<Note> search(String query) {
        return db.getNoteDao().search(query.trim());
    }

    public void delete(String id) {
        db.getFileInfoDao().getByNoteId(id).forEach(fileInfo -> {
            db.getDataBlockDao().deleteByFileId(fileInfo.getId());
            db.getFileInfoDao().deleteById(fileInfo.getId());
        });

        db.getNoteDao().deleteById(id);
    }

    private Note setDecimalId(Note note) {
        UUID uuid = UUID.fromString(note.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        note.setDecimalId(hash);

        return note;
    }
}
