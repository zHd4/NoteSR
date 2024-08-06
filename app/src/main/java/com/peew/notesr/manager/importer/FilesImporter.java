package com.peew.notesr.manager.importer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.table.DataBlocksTable;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class FilesImporter extends BaseImporter {

    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;

    public FilesImporter(JsonParser parser,
                         FilesInfoTable filesInfoTable,
                         DataBlocksTable dataBlocksTable,
                         DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);

        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
    }

    public void importFiles() throws IOException {
        importFilesInfo();
    }

    private void importFilesInfo() throws IOException {
        String field;
        skipTo("files_info");

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

            EncryptedFileInfo encryptedFileInfo = FilesCrypt.encryptInfo(fileInfo);
            filesInfoTable.save(encryptedFileInfo);
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }

    private void importFilesData() throws IOException {
        String field;
        skipTo("files_data_blocks");

        do {
            DataBlock dataBlock = new DataBlock();

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                field = parser.getCurrentName();

                if (field != null) {
                    switch (field) {
                        case "id" -> {
                            if (parser.getValueAsString().equals("id")) continue;
                            dataBlock.setId(parser.getValueAsLong());
                        }

                        case "file_id" -> {
                            if (parser.getValueAsString().equals("file_id")) continue;
                            dataBlock.setFileId(parser.getValueAsLong());
                        }

                        case "order" -> {
                            if (parser.getValueAsString().equals("order")) continue;
                            dataBlock.setOrder(parser.getValueAsLong());
                        }

                        case "data" -> {
                            byte[] data = parser.getBinaryValue();
                            System.out.println();
                        }

                        default -> {}
                    }
                }

                System.out.println();
            }
        } while (parser.nextToken() != JsonToken.END_ARRAY);
    }
}
