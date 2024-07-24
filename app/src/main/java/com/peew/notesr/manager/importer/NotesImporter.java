package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;
import com.peew.notesr.tools.ProgressCounter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class NotesImporter {
    private final JsonParser parser;
    private final ProgressCounter progressCounter;

    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;

    public NotesImporter(JsonParser parser,
                         ProgressCounter progressCounter,
                         NotesTable notesTable,
                         DateTimeFormatter timestampFormatter) {
        this.parser = parser;
        this.progressCounter = progressCounter;
        this.notesTable = notesTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void importNotes() throws IOException {
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String field = parser.getCurrentName();

                Long id = null;
                String name = null;
                String text = null;
                LocalDateTime updatedAt = null;

                switch (field) {
                    case "id":
                        id = parser.getLongValue();
                        break;
                    case "name":
                        name = parser.getText();
                        break;
                    case "text":
                        text = parser.getText();
                        break;
                    case "updated_at":
                        updatedAt = LocalDateTime.parse(parser.getText(), timestampFormatter);
                    default:
                        break;
                }

                Note note = new Note(name, text, updatedAt);
                EncryptedNote encryptedNote = NotesCrypt.encrypt(note);

                notesTable.save(encryptedNote);
            }
        }
    }
}
