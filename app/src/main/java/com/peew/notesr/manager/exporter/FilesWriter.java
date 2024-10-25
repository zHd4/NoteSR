package com.peew.notesr.manager.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.table.DataBlocksTable;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Set;

class FilesWriter implements Writer {
    private final JsonGenerator jsonGenerator;
    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;
    private final DateTimeFormatter timestampFormatter;
    private final long total;

    private long exported = 0;

    public FilesWriter(JsonGenerator jsonGenerator,
                       FilesInfoTable filesInfoTable,
                       DataBlocksTable dataBlocksTable,
                       DateTimeFormatter timestampFormatter) {
        this.jsonGenerator = jsonGenerator;
        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
        this.timestampFormatter = timestampFormatter;

        this.total = filesInfoTable.getRowsCount() + dataBlocksTable.getRowsCount();
    }

    public void writeFiles() throws IOException {
        Set<Long> filesId = filesInfoTable.getAllIds();

        writeFilesInfo(filesId);
        writeFilesData(filesId);
    }

    private void writeFilesInfo(Set<Long> filesId) throws IOException {
        jsonGenerator.writeArrayFieldStart("files_info");

        for (Long id : filesId) {
            FileInfo fileInfo = FilesCrypt.decryptInfo(filesInfoTable.get(id));
            writeFileInfo(fileInfo);

            exported++;
        }

        jsonGenerator.writeEndArray();
    }

    private void writeFilesData(Set<Long> filesId) throws IOException {
        jsonGenerator.writeArrayFieldStart("files_data_blocks");

        for (Long fileId : filesId) {
            Set<Long> blocksId = dataBlocksTable.getBlocksIdsByFileId(fileId);

            for (Long blockId : blocksId) {
                DataBlock block = dataBlocksTable.get(blockId);

                block.setData(FilesCrypt.decryptData(block.getData()));
                writeDataBlock(block);

                exported++;
            }
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

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public long getExported() {
        return exported;
    }
}
