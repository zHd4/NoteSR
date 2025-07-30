package app.notesr.service.data.exporter;

import com.fasterxml.jackson.core.JsonGenerator;

import app.notesr.db.dao.DataBlockDao;
import app.notesr.db.dao.FileInfoDao;
import app.notesr.model.DataBlock;
import app.notesr.model.FileInfo;
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
    private final FileInfoDao fileInfoDao;
    private final DataBlockDao dataBlockDao;
    private final Runnable checkCancelled;
    private final DateTimeFormatter timestampFormatter;

    @Getter
    private long exported = 0;


    @Override
    public void export() throws IOException{
        try (jsonGenerator) {
            jsonGenerator.writeStartObject();

            writeFilesInfo(fileInfoDao.getAll());
            writeDataBlocksInfo(dataBlockDao.getAllWithoutData());

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
        return fileInfoDao.getRowsCount() + dataBlockDao.getRowsCount();
    }
}
