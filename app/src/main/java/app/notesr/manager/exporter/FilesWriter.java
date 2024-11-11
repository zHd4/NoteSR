package app.notesr.manager.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.model.DataBlock;
import app.notesr.model.FileInfo;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@RequiredArgsConstructor
class FilesWriter implements Writer {
    @NonNull
    @Getter
    private final JsonGenerator jsonGenerator;

    @NonNull
    private final FilesInfoTable filesInfoTable;

    @NonNull
    private final DataBlocksTable dataBlocksTable;

    @NonNull
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private long exported = 0;

    @Override
    public void write() throws IOException {
        jsonGenerator.writeStartObject();
        Set<Long> filesId = filesInfoTable.getAllIds();

        writeFilesInfo(filesId);
        writeDataBlocksInfo(filesId);
        jsonGenerator.writeEndObject();
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

    private void writeDataBlocksInfo(Set<Long> filesId) throws IOException {
        jsonGenerator.writeArrayFieldStart("files_data_blocks");

        for (Long fileId : filesId) {
            Set<Long> blocksId = dataBlocksTable.getBlocksIdsByFileId(fileId);

            for (Long blockId : blocksId) {
                DataBlock block = dataBlocksTable.get(blockId);

                block.setData(FilesCrypt.decryptData(block.getData()));
                writeDataBlockInfo(block);

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

    private void writeDataBlockInfo(DataBlock dataBlock) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeNumberField("id", dataBlock.getId());
        jsonGenerator.writeNumberField("file_id", dataBlock.getFileId());
        jsonGenerator.writeNumberField("order", dataBlock.getOrder());

        jsonGenerator.writeEndObject();
    }

    @Override
    public long getTotal() {
        return filesInfoTable.getRowsCount() + dataBlocksTable.getRowsCount();
    }
}
