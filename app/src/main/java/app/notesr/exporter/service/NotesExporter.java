package app.notesr.exporter.service;

import com.fasterxml.jackson.core.JsonGenerator;

import app.notesr.db.dao.NoteDao;
import app.notesr.note.model.Note;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class NotesExporter implements Exporter {

    @Getter
    private final JsonGenerator jsonGenerator;
    private final NoteDao noteDao;
    private final Runnable checkCancelled;
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private long exported = 0;

    @Override
    public void export() throws IOException {
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart("notes");

            for (Note note : noteDao.getAll()) {
                writeNote(note);

                exported++;
                checkCancelled.run();
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

        String createdAt = note.getCreatedAt().format(timestampFormatter);
        jsonGenerator.writeStringField("created_at", createdAt);

        String updatedAt = note.getUpdatedAt().format(timestampFormatter);
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }

    @Override
    public long getTotal() {
        return noteDao.getRowsCount();
    }
}
