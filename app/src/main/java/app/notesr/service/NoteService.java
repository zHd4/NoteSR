package app.notesr.service;

import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.table.DataBlockDao;
import app.notesr.db.notes.table.FileInfoDao;
import app.notesr.model.EncryptedNote;
import app.notesr.dto.Note;
import app.notesr.util.HashHelper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class NoteService extends ServiceBase {
    public void save(Note note) {
        EncryptedNote encryptedNote = NoteCryptor.encrypt(note);
        getNoteTable().save(encryptedNote);
    }

    public List<Note> getAll() {
        return getNoteTable()
                .getAll()
                .stream()
                .map(NoteCryptor::decrypt)
                .map(this::setDecimalId)
                .collect(Collectors.toList());
    }

    public Note get(String id) {
        EncryptedNote encryptedNote = getNoteTable().get(id);

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
        FileInfoDao fileInfoTable = getFileInfoTable();
        DataBlockDao dataBlockTable = getDataBlockTable();

        fileInfoTable.getByNoteId(id).forEach(fileInfo -> {
            dataBlockTable.deleteByFileId(fileInfo.getId());
            fileInfoTable.delete(fileInfo.getId());
        });

        getNoteTable().delete(id);
    }

    private Note setDecimalId(Note note) {
        UUID uuid = UUID.fromString(note.getId());
        long hash = HashHelper.getUUIDHash(uuid);

        note.setDecimalId(hash);

        return note;
    }
}
