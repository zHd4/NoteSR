package app.notesr.manager.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.NotesTable;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class NotesWriter implements Writer {

    private final JsonGenerator jsonGenerator;
    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;
    private final long totalNotes;

    private long exported;

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

            exported++;
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
    public long getTotal() {
        return totalNotes;
    }

    @Override
    public long getExported() {
        return exported;
    }
}
