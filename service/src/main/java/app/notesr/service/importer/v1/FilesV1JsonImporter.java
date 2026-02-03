/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer.v1;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.service.file.FileService;
import app.notesr.service.importer.BaseFilesJsonImporter;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;

final class FilesV1JsonImporter extends BaseFilesJsonImporter {

    FilesV1JsonImporter(JsonParser parser,
                               FileService fileService,
                               Map<String, String> adaptedNotesIdMap,
                               DateTimeFormatter timestampFormatter) {

        super(parser, fileService, adaptedNotesIdMap, timestampFormatter);
    }

    @Override
    protected void importFilesData() throws IOException, EncryptionFailedException {
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

    private AbstractMap.SimpleEntry<FileBlobInfo, byte[]> parseDataBlockObject()
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
                        adaptFileBlobInfoId(genericBlobInfo);
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

                    default -> throw new IllegalStateException("Unexpected value: " + field);
                }
            }
        }

        return new AbstractMap.SimpleEntry<>(genericBlobInfo, genericBlobBytes);
    }
}
