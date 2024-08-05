package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class NotesImporter {

    private final JsonParser parser;
    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;

    public NotesImporter(JsonParser parser,
                         NotesTable notesTable,
                         DateTimeFormatter timestampFormatter) {
        this.parser = parser;
        this.notesTable = notesTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void importNotes() throws IOException {
        String field = parser.getCurrentName();

        while (field == null || field.equals("version") || field.equals("notes")) {
            parser.nextToken();
            field = parser.getCurrentName();
        }

        do {
            Note note = new Note();

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                field = parser.getCurrentName();

                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;
                        note.setId(parser.getValueAsLong());
                    }

                    case "name" -> {
                        if (parser.getValueAsString().equals("name")) continue;
                        note.setName(parser.getValueAsString());
                    }

                    case "text" -> {
                        if (parser.getValueAsString().equals("text")) continue;
                        note.setText(parser.getValueAsString());
                    }

                    case "updated_at" -> {
                        if (parser.getValueAsString().equals("updated_at")) continue;
                        note.setUpdatedAt(LocalDateTime.parse(parser.getValueAsString(), timestampFormatter));
                    }

                    default -> {}
                }
            }

            EncryptedNote encryptedNote = NotesCrypt.encrypt(note);
            notesTable.save(encryptedNote);
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }
}
