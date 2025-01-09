package app.notesr.service.data.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.NotesTable;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;
import lombok.Getter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class NotesExporter extends Exporter {
    @Getter
    private final JsonGenerator jsonGenerator;

    private final NotesTable notesTable;
    private final DateTimeFormatter timestampFormatter;

    NotesExporter(ExportThread thread,
                  JsonGenerator jsonGenerator,
                  NotesTable notesTable,
                  DateTimeFormatter timestampFormatter) {
        super(thread);

        this.jsonGenerator = jsonGenerator;
        this.notesTable = notesTable;
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public void export() throws IOException, InterruptedException {
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart("notes");

            for (EncryptedNote encryptedNote : notesTable.getAll()) {
                Note note = NotesCrypt.decrypt(encryptedNote);
                writeNote(note);

                increaseExported();
                getThread().breakOnInterrupted();
            }

            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
    }

    private void writeNote(Note note) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("id", note.getId());

        jsonGenerator.writeStringField("name", note.getName());
        jsonGenerator.writeStringField("text", note.getText());

        String updatedAt = note.getUpdatedAt().format(timestampFormatter);
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }

    @Override
    long getTotal() {
        return notesTable.getRowsCount();
    }
}
