package com.peew.notesr.manager.export;

import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.table.DataBlocksTable;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class FilesWriter {
    private final JsonGenerator jsonGenerator;
    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;
    private final DateTimeFormatter timestampFormatter;

    public FilesWriter(JsonGenerator jsonGenerator,
                       FilesInfoTable filesInfoTable,
                       DataBlocksTable dataBlocksTable,
                       DateTimeFormatter timestampFormatter) {
        this.jsonGenerator = jsonGenerator;
        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
        this.timestampFormatter = timestampFormatter;
    }

    public void writeFiles() throws IOException {
        jsonGenerator.writeArrayFieldStart("assignments");

        writeFilesInfo();

        jsonGenerator.writeEndArray();
    }

    private void writeFilesInfo() throws IOException {
        jsonGenerator.writeArrayFieldStart("files_info");

        Set<Long> filesId = filesInfoTable.getAllIds();

        for (Long id : filesId) {
            FileInfo fileInfo = FilesCrypt.decryptInfo(filesInfoTable.get(id));
            writeFileInfo(fileInfo);
        }


        jsonGenerator.writeEndArray();
    }

    private void writeFileInfo(FileInfo fileInfo) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeNumberField("id", fileInfo.getId());
        jsonGenerator.writeNumberField("note_id", fileInfo.getNoteId());
        jsonGenerator.writeNumberField("size", fileInfo.getSize());

        jsonGenerator.writeStringField("name", fileInfo.getName());
        jsonGenerator.writeStringField("type", fileInfo.getType());

        String createdAt = fileInfo.getCreatedAt().format(timestampFormatter);
        String updatedAt = fileInfo.getUpdatedAt().format(timestampFormatter);

        jsonGenerator.writeStringField("created_at", createdAt);
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }

    private void writeDataBlock(DataBlock dataBlock) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeNumberField("id", dataBlock.getId());
        jsonGenerator.writeNumberField("file_id", dataBlock.getFileId());
        jsonGenerator.writeNumberField("order", dataBlock.getOrder());

        jsonGenerator.writeBinaryField("data", dataBlock.getData());

        jsonGenerator.writeEndObject();
    }
}
