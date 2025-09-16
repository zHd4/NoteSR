package app.notesr.importer.service.v2;

import static java.util.Objects.requireNonNull;

import android.net.Uri;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.BaseFilesJsonImporter;

class FilesV2JsonImporter extends BaseFilesJsonImporter {

    private final File dataBlocksDir;

    public FilesV2JsonImporter(JsonParser parser,
                               FileService fileService,
                               Map<String, String> adaptedNotesIdMap,
                               File dataBlocksDir,
                               DateTimeFormatter timestampFormatter) {
        super(parser, fileService, adaptedNotesIdMap, timestampFormatter);
        this.dataBlocksDir = dataBlocksDir;
    }

    @Override
    protected void importFilesData() throws IOException, DecryptionFailedException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    FileBlobInfo genericBlobInfo = parseDataBlockObject();
                    String id = genericBlobInfo.getId();

                    if (fileService.getFileBlobInfo(id) == null) {
                        String fileId = genericBlobInfo.getFileId();
                        String genericBlobFileName = adaptedDataBlocksIdMap.getOrDefault(id, id);

                        requireNonNull(genericBlobFileName,
                                "Generic blob file name is null");

                        File genericBlobFile = new File(dataBlocksDir, genericBlobFileName);
                        fileService.saveFileData(fileId, Uri.fromFile(genericBlobFile));
                    }
                } while (parser.nextToken() != JsonToken.END_ARRAY);
            }
        }
    }

    protected FileBlobInfo parseDataBlockObject() throws IOException {
        FileBlobInfo dataBlock = new FileBlobInfo();
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;

                        dataBlock.setId(parser.getValueAsString());
                        adaptId(dataBlock);
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

                    default -> {}
                }
            }
        }

        return dataBlock;
    }
}
