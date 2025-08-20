package app.notesr.exporter.service;

import com.fasterxml.jackson.core.JsonGenerator;

import app.notesr.file.model.DataBlock;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class FilesInfoExporter implements Exporter {

    @Getter
    private final JsonGenerator jsonGenerator;
    private final FileService fileService;
    private final Runnable checkCancelled;
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private long exported = 0;


    @Override
    public void export() throws IOException {
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();

            writeFilesInfo(fileService.getAllFilesInfo());
            writeDataBlocksInfo(fileService.getAllDataBlocksWithoutData());

            jsonGenerator.writeEndObject();
        }
    }

    private void writeFilesInfo(List<FileInfo> fileInfos) throws IOException {
        jsonGenerator.writeArrayFieldStart("files_info");

        for (FileInfo fileInfo : fileInfos) {
            writeFileInfo(fileInfo);

            exported++;
            checkCancelled.run();
        }

        jsonGenerator.writeEndArray();
    }

    private void writeDataBlocksInfo(List<DataBlock> dataBlocks) throws IOException {
        jsonGenerator.writeArrayFieldStart("files_data_blocks");

        for (DataBlock dataBlock : dataBlocks) {
            writeDataBlockInfo(dataBlock);

            exported++;
            checkCancelled.run();
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

    @Override
    public long getTotal() {
        return fileService.getFilesCount() + fileService.getDataBlocksCount();
    }
}
