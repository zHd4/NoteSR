package app.notesr.manager.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.NotesTable;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
class NotesWriter implements JsonWriter {
    @NonNull
    @Getter
    private final JsonGenerator jsonGenerator;

    @NonNull
    private final NotesTable notesTable;

    @NonNull
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private long exported;

    @Override
    public void write() throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeArrayFieldStart("notes");

        for (EncryptedNote encryptedNote : notesTable.getAll()) {
            Note note = NotesCrypt.decrypt(encryptedNote);
            writeNote(note);

            exported++;
        }

        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndObject();
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
        return notesTable.getRowsCount();
    }
}
