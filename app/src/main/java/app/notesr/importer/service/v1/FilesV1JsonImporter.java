package app.notesr.importer.service.v1;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.service.FileService;
import app.notesr.importer.service.BaseFilesJsonImporter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;

class FilesV1JsonImporter extends BaseFilesJsonImporter {

    public FilesV1JsonImporter(JsonParser parser,
                               FileService fileService,
                               Map<String, String> adaptedNotesIdMap,
                               DateTimeFormatter timestampFormatter) {

        super(parser, fileService, adaptedNotesIdMap, timestampFormatter);
    }

    @Override
    protected void importFilesData() throws IOException {
        if (skipTo("files_data_blocks")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    AbstractMap.SimpleEntry<FileBlobInfo, byte[]> blob = parseDataBlockObject();

                    FileBlobInfo blobInfo = blob.getKey();
                    byte[] blobData = blob.getValue();

                    if (fileService.getFileBlobInfo(blobInfo.getId()) == null) {
                        fileService.importFileBlobInfo(blobInfo);
                        fileService.importFileBlobData(blobInfo.getId(), blobData);
                    }
                } while (parser.nextToken() != JsonToken.END_ARRAY);
            }
        }
    }

    protected AbstractMap.SimpleEntry<FileBlobInfo, byte[]> parseDataBlockObject()
            throws IOException {

        FileBlobInfo genericBlobInfo = new FileBlobInfo();

        byte[] genericBlobBytes = null;
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;

                        genericBlobInfo.setId(parser.getValueAsString());
                        adaptId(genericBlobInfo);
                    }

                    case "file_id" -> {
                        if (parser.getValueAsString().equals("file_id")) continue;

                        String id = parser.getValueAsString();

                        genericBlobInfo.setFileId(
                                requireNonNull(adaptedFilesIdMap.getOrDefault(id, id))
                        );
                    }

                    case "order" -> {
                        if (parser.getValueAsString().equals("order")) continue;
                        genericBlobInfo.setOrder(parser.getValueAsLong());
                    }

                    case "data" -> {
                        if (!parser.getValueAsString().equals("data")) {
                            genericBlobBytes = parser.getBinaryValue();
                        }
                    }

                    default -> {}
                }
            }
        }

        return new AbstractMap.SimpleEntry<>(genericBlobInfo, genericBlobBytes);
    }
}
