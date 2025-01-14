package app.notesr.service.data.exporter;

import com.fasterxml.jackson.core.JsonGenerator;
import app.notesr.crypto.FilesCrypt;
import app.notesr.db.notes.table.DataBlocksTable;
import app.notesr.db.notes.table.FilesInfoTable;
import app.notesr.model.DataBlock;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.FileInfo;
import lombok.Getter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

class FilesInfoExporter extends Exporter {
    @Getter
    private final JsonGenerator jsonGenerator;

    private final FilesInfoTable filesInfoTable;
    private final DataBlocksTable dataBlocksTable;

    private final DateTimeFormatter timestampFormatter;

    FilesInfoExporter(ExportThread thread,
                      JsonGenerator jsonGenerator,
                      FilesInfoTable filesInfoTable,
                      DataBlocksTable dataBlocksTable,
                      DateTimeFormatter timestampFormatter) {
        super(thread);

        this.jsonGenerator = jsonGenerator;
        this.filesInfoTable = filesInfoTable;
        this.dataBlocksTable = dataBlocksTable;
        this.timestampFormatter = timestampFormatter;
    }

    @Override
    public void export() throws IOException, InterruptedException {
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();

            writeFilesInfo(filesInfoTable.getAll());
            writeDataBlocksInfo(dataBlocksTable.getAllWithoutData());

            jsonGenerator.writeEndObject();
        }
    }

    @Override
    long getTotal() {
        return filesInfoTable.getRowsCount() + dataBlocksTable.getRowsCount();
    }

    private void writeFilesInfo(List<EncryptedFileInfo> encryptedFilesInfo) throws IOException,
            InterruptedException {
        jsonGenerator.writeArrayFieldStart("files_info");

        for (EncryptedFileInfo encryptedFileInfo : encryptedFilesInfo) {
            FileInfo fileInfo = FilesCrypt.decryptInfo(encryptedFileInfo);
            writeFileInfo(fileInfo);

            increaseExported();
            getThread().breakOnInterrupted();
        }

        jsonGenerator.writeEndArray();
    }

    private void writeDataBlocksInfo(List<DataBlock> dataBlocks) throws IOException,
            InterruptedException {
        jsonGenerator.writeArrayFieldStart("files_data_blocks");

        for (DataBlock dataBlock : dataBlocks) {
            writeDataBlockInfo(dataBlock);

            increaseExported();
            getThread().breakOnInterrupted();
        }

        jsonGenerator.writeEndArray();
    }

    private void writeFileInfo(FileInfo fileInfo) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("id", fileInfo.getId());
        jsonGenerator.writeStringField("note_id", fileInfo.getNoteId());
        jsonGenerator.writeNumberField("size", fileInfo.getSize());

        jsonGenerator.writeStringField("name", fileInfo.getName());
        jsonGenerator.writeStringField("type", fileInfo.getType());

        if (fileInfo.getThumbnail() != null) {
            jsonGenerator.writeBinaryField("thumbnail", fileInfo.getThumbnail());
        }

        String createdAt = fileInfo.getCreatedAt().format(timestampFormatter);
        String updatedAt = fileInfo.getUpdatedAt().format(timestampFormatter);

        jsonGenerator.writeStringField("created_at", createdAt);
        jsonGenerator.writeStringField("updated_at", updatedAt);

        jsonGenerator.writeEndObject();
    }

    private void writeDataBlockInfo(DataBlock dataBlock) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField("id", dataBlock.getId());
        jsonGenerator.writeStringField("file_id", dataBlock.getFileId());
        jsonGenerator.writeNumberField("order", dataBlock.getOrder());

        jsonGenerator.writeEndObject();
    }
}
