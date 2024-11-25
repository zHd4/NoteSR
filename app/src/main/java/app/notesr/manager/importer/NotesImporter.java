package app.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import app.notesr.crypto.NotesCrypt;
import app.notesr.db.notes.table.NotesTable;
import app.notesr.exception.ImportFailedException;
import app.notesr.model.EncryptedNote;
import app.notesr.model.Note;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotesImporter extends BaseImporter {

    private final NotesTable notesTable;

    public NotesImporter(JsonParser parser,
                         NotesTable notesTable,
                         DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);
        this.notesTable = notesTable;
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
                    switch (field) {
                        case "id" -> {
                            if (parser.getValueAsString().equals("id")) continue;
                            note.setId(parser.getValueAsLong());
                        }

                        case "name" -> {
                            if (parser.getValueAsString().equals("name")) continue;
                            note.setName(parser.getValueAsString());
                        }

                        case "text" -> {
                            if (parser.getValueAsString().equals("text")) continue;
                            note.setText(parser.getValueAsString());
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

            if (note.getUpdatedAt() == null) {
                note.setUpdatedAt(LocalDateTime.now());
            }

            EncryptedNote encryptedNote = NotesCrypt.encrypt(note);
            notesTable.importNote(encryptedNote);
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }
}
