package com.peew.notesr.manager.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class NotesExporter {

    private final JsonGenerator jsonGenerator;
    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;

    public NotesExporter(JsonGenerator jsonGenerator, NotesTable notesTable, DateTimeFormatter timestampFormatter) {
        this.jsonGenerator = jsonGenerator;
        this.notesTable = notesTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void writeNotes() throws IOException {
        jsonGenerator.writeArrayFieldStart("notes");

        for (EncryptedNote encryptedNote : notesTable.getAll()) {
            Note note = NotesCrypt.decrypt(encryptedNote);
            writeNote(note);
        }

        jsonGenerator.writeEndArray();
    }

    private void writeNote(Note note) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeFieldId(note.getId());

        jsonGenerator.writeStringField("name", note.getName());
        jsonGenerator.writeStringField("text", note.getText());

        String updatedAt = note.getUpdatedAt().format(timestampFormatter);
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }
}
