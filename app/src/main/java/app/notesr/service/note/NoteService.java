package app.notesr.service.note;

import app.notesr.db.dao.DataBlockDao;
import app.notesr.db.dao.FileInfoDao;
import app.notesr.db.dao.NoteDao;
import app.notesr.model.Note;
import app.notesr.util.HashHelper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NoteService {
    private final NoteDao noteDao;
    private final FileInfoDao fileInfoDao;
    private final DataBlockDao dataBlockDao;

    public void save(Note note) {
        note.setUpdatedAt(LocalDateTime.now());
        noteDao.insert(note);
    }

    public List<Note> getAll() {
        return noteDao
                .getAll()
                .stream()
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public Note get(String id) {
        Note note = noteDao.get(id);

        if (note != null) {
            setDecimalId(note);
            return note;
        }

        return null;
    }

    public List<String> search(String query) {
        return getAll().stream()
                .filter(note -> {
                    String formattedQuery = query.trim().toLowerCase();

                    boolean foundInName = note.getName()
                            .toLowerCase()
                            .contains(formattedQuery);

                    boolean foundInText = note.getText()
                            .toLowerCase()
                            .contains(formattedQuery);

                    return foundInName || foundInText;
                })
                .map(Note::getId)
                .collect(Collectors.toList());
    }

    public void delete(String id) {
        fileInfoDao.getByNoteId(id).forEach(fileInfo -> {
            dataBlockDao.deleteByFileId(fileInfo.getId());
            fileInfoDao.deleteById(fileInfo.getId());
        });

        noteDao.deleteById(id);
    }

    private Note setDecimalId(Note note) {
        UUID uuid = UUID.fromString(note.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        note.setDecimalId(hash);

        return note;
    }
}
