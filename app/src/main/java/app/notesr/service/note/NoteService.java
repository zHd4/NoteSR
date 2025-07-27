package app.notesr.service.note;

import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;
import app.notesr.util.HashHelper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class NoteService {
    private final NoteDao noteDao;
    private final FileInfoDao fileInfoDao;
    private final DataBlockDao dataBlockDao;

    public void save(Note note) {
        EncryptedNote encryptedNote = NoteCryptor.encrypt(note);
        noteDao.save(encryptedNote);
    }

    public List<Note> getAll() {
        return noteDao
                .getAll()
                .stream()
                .map(NoteCryptor::decrypt)
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public Note get(String id) {
        EncryptedNote encryptedNote = noteDao.get(id);

        if (encryptedNote != null) {
            Note note = NoteCryptor.decrypt(encryptedNote);
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
            fileInfoDao.delete(fileInfo.getId());
        });

        noteDao.delete(id);
    }

    private Note setDecimalId(Note note) {
        UUID uuid = UUID.fromString(note.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        note.setDecimalId(hash);

        return note;
    }
}
