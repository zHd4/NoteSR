package app.notesr.importer.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class NotesJsonImporter extends BaseJsonImporter {

    @Getter
    private final Map<String, String> adaptedIdMap = new HashMap<>();

    private final NoteService noteService;

    public NotesJsonImporter(JsonParser parser,
                             NoteService noteService,
                             DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);
        this.noteService = noteService;
    }

    public void importNotes() throws IOException, ImportFailedException {
        String field;

        if (!skipTo("notes")) {
            throw new ImportFailedException("'notes' field not found in json");
        }

        do {
            Note note = parseNoteObject();
            noteService.importNote(note);
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }

    private Note parseNoteObject() throws IOException {
        Note note = new Note();
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;
                        note.setId(adaptId(parser.getValueAsString()));
                    }

                    case "name" -> {
                        if (parser.getValueAsString().equals("name")) continue;
                        note.setName(parser.getValueAsString());
                    }

                    case "text" -> {
                        if (parser.getValueAsString().equals("text")) continue;
                        note.setText(parser.getValueAsString());
                    }

                    case "created_at" -> {
                        if (parser.getValueAsString().equals("created_at")) continue;

                        LocalDateTime createdAt = LocalDateTime.parse(
                                parser.getValueAsString(),
                                timestampFormatter
                        );

                        note.setCreatedAt(createdAt);
                    }

                    case "updated_at" -> {
                        if (parser.getValueAsString().equals("updated_at")) continue;

                        LocalDateTime updatedAt = LocalDateTime.parse(
                                parser.getValueAsString(),
                                timestampFormatter
                        );

                        note.setUpdatedAt(updatedAt);
                    }

                    default -> {}
                }
            }
        }

        return note;
    }

    private String adaptId(String id) {
        String adaptedId = new IdAdapter(id).getId();

        if (!adaptedId.equals(id)) {
            adaptedIdMap.put(id, adaptedId);
        }

        return adaptedId;
    }
}
