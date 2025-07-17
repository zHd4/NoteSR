package app.notesr.service.data.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.model.EncryptedNote;
import app.notesr.dto.Note;
import lombok.Getter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class NotesExporter extends BaseExporter {
    @Getter
    private final JsonGenerator jsonGenerator;

    private final NoteDao noteTable;
    private final DateTimeFormatter timestampFormatter;

    NotesExporter(ExportThread thread,
                  JsonGenerator jsonGenerator,
                  NoteDao noteTable,
                  DateTimeFormatter timestampFormatter) {
        super(thread);

        this.jsonGenerator = jsonGenerator;
        this.noteTable = noteTable;
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public void export() throws IOException, InterruptedException {
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart("notes");

            for (EncryptedNote encryptedNote : noteTable.getAll()) {
                Note note = NoteCryptor.decrypt(encryptedNote);
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
        return noteTable.getRowsCount();
    }
}
