package app.notesr.manager.importer.v2;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.manager.importer.BaseImporter;
import app.notesr.model.FileInfo;
import app.notesr.utils.FilesUtils;

class FilesImporter extends BaseImporter {

    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;

    private final File dataBlocksDir;

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         File dataBlocksDir,
                         DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);

        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
        this.dataBlocksDir = dataBlocksDir;
    }

    private void parseFileInfoObject(FileInfo fileInfo) throws IOException {
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;
                        fileInfo.setId(parser.getValueAsLong());
                    }

                    case "note_id" -> {
                        if (parser.getValueAsString().equals("note_id")) continue;
                        fileInfo.setNoteId(parser.getValueAsLong());
                    }

                    case "size" -> {
                        if (parser.getValueAsString().equals("size")) continue;
                        fileInfo.setSize(parser.getValueAsLong());
                    }

                    case "name" -> {
                        if (parser.getValueAsString().equals("name")) continue;
                        fileInfo.setName(parser.getValueAsString());
                    }

                    case "type" -> {
                        if (parser.getValueAsString().equals("type")) continue;
                        fileInfo.setType(parser.getValueAsString());
                    }

                    case "created_at" -> {
                        if (parser.getValueAsString().equals("created_at")) continue;

                        fileInfo.setCreatedAt(
                                LocalDateTime.parse(parser.getValueAsString(), timestampFormatter)
                        );
                    }

                    case "updated_at" -> {
                        if (parser.getValueAsString().equals("updated_at")) continue;

                        fileInfo.setUpdatedAt(
                                LocalDateTime.parse(parser.getValueAsString(), timestampFormatter)
                        );
                    }

                    default -> {}
                }
            }
        }
    }

    private byte[] readDataBlock(Long id) throws IOException {
        return FilesUtils.readFileBytes(new File(dataBlocksDir, id.toString()));
    }
}
