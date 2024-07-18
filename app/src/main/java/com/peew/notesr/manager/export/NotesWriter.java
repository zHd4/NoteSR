package com.peew.notesr.manager.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class NotesWriter implements Writer {

    private final JsonGenerator jsonGenerator;
    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;
    private final long totalNotes;

    private long done;

    public NotesWriter(JsonGenerator jsonGenerator, NotesTable notesTable, DateTimeFormatter timestampFormatter) {
        this.jsonGenerator = jsonGenerator;
        this.notesTable = notesTable;
        this.timestampFormatter = timestampFormatter;

        this.totalNotes = notesTable.getRowsCount();
    }

    public void writeNotes() throws IOException {
        jsonGenerator.writeArrayFieldStart("notes");

        for (EncryptedNote encryptedNote : notesTable.getAll()) {
            Note note = NotesCrypt.decrypt(encryptedNote);
            writeNote(note);

            done++;
        }

        jsonGenerator.writeEndArray();
    }

    private void writeNote(Note note) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeNumberField("id", note.getId());

        jsonGenerator.writeStringField("name", note.getName());
        jsonGenerator.writeStringField("text", note.getText());

        String updatedAt = note.getUpdatedAt().format(timestampFormatter);
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }

    @Override
    public int getProgress() {
        return Math.round((done * 100.0f) / totalNotes);
    }
}
