package app.notesr.importer.service.v2;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.exception.EncryptionFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.BaseFilesJsonImporter;

final class FilesV2JsonImporter extends BaseFilesJsonImporter {

    private final File dataBlocksDir;

    FilesV2JsonImporter(JsonParser parser,
                               FileService fileService,
                               Map<String, String> adaptedNotesIdMap,
                               File dataBlocksDir,
                               DateTimeFormatter timestampFormatter) {
        super(parser, fileService, adaptedNotesIdMap, timestampFormatter);
        this.dataBlocksDir = dataBlocksDir;
    }

    @Override
    protected void importFilesData() throws IOException, EncryptionFailedException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    FileBlobInfo blobInfo = parseDataBlockObject();
                    String blobId = blobInfo.getId();

                    if (fileService.getFileBlobInfo(blobId) == null) {
                        String tempBlobDataFileName =
                                adaptedDataBlocksIdMap.getOrDefault(blobId, blobId);

                        requireNonNull(tempBlobDataFileName,
                                "Generic blob file name is null");

                        File tempBlobDataFile = new File(dataBlocksDir, tempBlobDataFileName);
                        byte[] blobData = Files.readAllBytes(tempBlobDataFile.toPath());

                        fileService.importFileBlobInfo(blobInfo);
                        fileService.importFileBlobData(blobId, blobData);
                    }
                } while (parser.nextToken() != JsonToken.END_ARRAY);
            }
        }
    }

    private FileBlobInfo parseDataBlockObject() throws IOException {
        FileBlobInfo dataBlock = new FileBlobInfo();
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;

                        dataBlock.setId(parser.getValueAsString());
                        adaptFileBlobInfoId(dataBlock);
                    }

                    case "file_id" -> {
                        if (parser.getValueAsString().equals("file_id")) continue;

                        String id = parser.getValueAsString();
                        dataBlock.setFileId(requireNonNull(adaptedFilesIdMap.getOrDefault(id, id)));
                    }

                    case "order" -> {
                        if (parser.getValueAsString().equals("order")) continue;
                        dataBlock.setOrder(parser.getValueAsLong());
                    }

                    default -> throw new IllegalStateException("Unexpected value: " + field);
                }
            }
        }

        return dataBlock;
    }
}
