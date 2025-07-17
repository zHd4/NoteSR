package app.notesr.service.data.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import app.notesr.crypto.NoteCryptor;
import app.notesr.db.notes.table.NoteDao;
import app.notesr.exception.ImportFailedException;
import app.notesr.model.EncryptedNote;
import app.notesr.dto.Note;
import lombok.Getter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class NotesImporter extends BaseImporter {

    @Getter
    private final Map<String, String> adaptedIdMap = new HashMap<>();

    private final NoteDao noteTable;

    public NotesImporter(JsonParser parser,
                         NoteDao noteTable,
                         DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);
        this.noteTable = noteTable;
    }

    public void importNotes() throws IOException, ImportFailedException {
        String field;

        if (!skipTo("notes")) {
            throw new ImportFailedException("'notes' field not found in json");
        }

        do {
            Note note = new Note();

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                field = parser.getCurrentName();

                if (field != null) {
                    parseNote(parser, note, field);
                }
            }

            if (note.getUpdatedAt() == null) {
                note.setUpdatedAt(LocalDateTime.now());
            }

            EncryptedNote encryptedNote = NoteCryptor.encrypt(note);
            noteTable.save(encryptedNote, false);
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }

    private void parseNote(JsonParser parser, Note note, String field) throws IOException {
        switch (field) {
            case "id" -> {
                if (parser.getValueAsString().equals("id")) return;
                note.setId(adaptId(parser.getValueAsString()));
            }

            case "name" -> {
                if (parser.getValueAsString().equals("name")) return;
                note.setName(parser.getValueAsString());
            }

            case "text" -> {
                if (parser.getValueAsString().equals("text")) return;
                note.setText(parser.getValueAsString());
            }

            case "updated_at" -> {
                if (parser.getValueAsString().equals("updated_at")) return;

                LocalDateTime updatedAt = LocalDateTime.parse(
                        parser.getValueAsString(),
                        timestampFormatter
                );

                note.setUpdatedAt(updatedAt);
            }

            default -> {}
        }
    }

    private String adaptId(String id) {
        String adaptedId = new IdAdapter(id).getId();

        if (!adaptedId.equals(id)) {
            adaptedIdMap.put(id, adaptedId);
        }

        return adaptedId;
    }
}
