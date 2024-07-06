package com.peew.notesr.manager;

import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

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
                    String lowerCaseQuery = query.toLowerCase();

                    boolean foundInName = note.getName()
                            .toLowerCase()
                            .contains(lowerCaseQuery);

                    boolean foundInText = note.getText()
                            .toLowerCase()
                            .contains(lowerCaseQuery);

                    return foundInName || foundInText;
                })
                .map(Note::getId)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        getNotesTable().delete(id);
    }
}
