package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.peew.notesr.db.notes.table.DataBlocksTable;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class FilesImporter {
    private final JsonParser parser;
    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;
    private final DateTimeFormatter timestampFormatter;

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         DateTimeFormatter timestampFormatter) {
        this.parser = parser;
        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void importFiles() throws IOException {

    }

    private void importFilesInfo() throws IOException {
        String field = parser.getCurrentName();

        while (field == null || !field.equals("files_info")) {
            parser.nextToken();
            field = parser.getCurrentName();
        }

        do {
            FileInfo fileInfo = new FileInfo();

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
                            fileInfo.setCreatedAt(LocalDateTime.parse(parser.getValueAsString(), timestampFormatter));
                        }

                        case "updated_at" -> {
                            if (parser.getValueAsString().equals("updated_at")) continue;
                            fileInfo.setUpdatedAt(LocalDateTime.parse(parser.getValueAsString(), timestampFormatter));
                        }

                        default -> {}
                    }
                }
            }
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }
}
