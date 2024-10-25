package app.notesr.manager;

import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;

import java.util.List;
import java.util.stream.Collectors;

public class NotesManager extends BaseManager {
    public void save(Note note) {
        EncryptedNote encryptedNote = NotesCrypt.encrypt(note);
        getNotesTable().save(encryptedNote);
    }

    public List<Note> getAll() {
        return getNotesTable()
                .getAll()
                .stream()
                .map(NotesCrypt::decrypt)
                .collect(Collectors.toList());
    }

    public Note get(Long id) {
        EncryptedNote encryptedNote = getNotesTable().get(id);

        return encryptedNote != null
                ? NotesCrypt.decrypt(encryptedNote)
                : null;
    }

    public List<Long> search(String query) {
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

    public void delete(Long id) {
        FilesInfoTable filesInfoTable = getFilesInfoTable();
        DataBlocksTable dataBlocksTable = getDataBlocksTable();

        filesInfoTable.getByNoteId(id).forEach(fileInfo -> {
            dataBlocksTable.deleteByFileId(fileInfo.getId());
            filesInfoTable.delete(fileInfo.getId());
        });

        getNotesTable().delete(id);
    }
}
