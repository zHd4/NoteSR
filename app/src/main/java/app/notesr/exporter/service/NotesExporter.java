package app.notesr.exporter.service;

import com.fasterxml.jackson.core.JsonGenerator;

import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import lombok.Getter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

class NotesExporter extends Exporter {

    @Getter
    private final JsonGenerator jsonGenerator;

    private final NoteService noteService;
    private final Runnable checkCancelled;
    private final DateTimeFormatter timestampFormatter;

    NotesExporter(JsonGenerator jsonGenerator,
                  NoteService noteService,
                  Runnable checkCancelled,
                  Runnable notifyProgress,
                  DateTimeFormatter timestampFormatter) {

        super(notifyProgress);

        this.jsonGenerator = jsonGenerator;
        this.noteService = noteService;
        this.checkCancelled = checkCancelled;
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public void export() throws IOException {
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeArrayFieldStart("notes");

            for (Note note : noteService.getAll()) {
                writeNote(note);

                increaseProgress();
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
        return noteService.getCount();
    }
}
