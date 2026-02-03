/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.data.model.FileBlobInfo;
import app.notesr.data.model.FileInfo;
import app.notesr.service.file.FileService;

public abstract class BaseFilesJsonImporter extends BaseJsonImporter {

    protected final FileService fileService;
    protected final Map<String, String> adaptedNotesIdMap;
    protected final Map<String, String> adaptedFilesIdMap = new HashMap<>();
    protected final Map<String, String> adaptedDataBlocksIdMap = new HashMap<>();

    public BaseFilesJsonImporter(JsonParser parser,
                                 FileService fileService,
                                 Map<String, String> adaptedNotesIdMap,
                                 DateTimeFormatter timestampFormatter) {
        super(parser, timestampFormatter);

        this.fileService = fileService;
        this.adaptedNotesIdMap = adaptedNotesIdMap;
    }

    public final void importFiles()
            throws IOException, EncryptionFailedException, DecryptionFailedException {
        importFilesInfo();
        importFilesData();
    }

    protected final void importFilesInfo() throws IOException {
        if (skipTo("files_info")) {
            if (parser.nextToken() == JsonToken.START_ARRAY) {
                do {
                    FileInfo fileInfo = parseFileInfoObject();
                    fileService.importFileInfo(fileInfo);
                } while (parser.nextToken() != JsonToken.END_ARRAY);
            }
        }
    }

    protected final FileInfo parseFileInfoObject() throws IOException {
        FileInfo fileInfo = new FileInfo();
        String field;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            field = parser.getCurrentName();

            if (field != null) {
                switch (field) {
                    case "id" -> {
                        if (parser.getValueAsString().equals("id")) continue;

                        fileInfo.setId(parser.getValueAsString());
                        adaptFileInfoId(fileInfo);
                    }

                    case "note_id" -> {
                        if (parser.getValueAsString().equals("note_id")) continue;

                        String id = parser.getValueAsString();
                        fileInfo.setNoteId(requireNonNull(adaptedNotesIdMap.getOrDefault(id, id)));
                    }

                    case "size" -> {
                        if (parser.getValueAsString().equals("size")) continue;
                        fileInfo.setSize(parser.getValueAsLong());
                    }

                    case "name" -> {
                        if (parser.getValueAsString().equals("name")) continue;
                        fileInfo.setName(parser.getValueAsString());
                    }

                    case "type" -> {
                        String value = parser.getValueAsString();

                        if (value != null) {
                            if (value.equals("type")) continue;
                            fileInfo.setType(parser.getValueAsString());
                        }
                    }

                    case "thumbnail" -> {
                        String value = parser.getValueAsString();

                        if (value != null) {
                            if (value.equals("thumbnail")) continue;
                            fileInfo.setThumbnail(parser.getBinaryValue());
                        }
                    }

                    case "created_at" -> {
                        if (parser.getValueAsString().equals("created_at")) continue;

                        fileInfo.setCreatedAt(
                                LocalDateTime.parse(parser.getValueAsString(), timestampFormatter)
                        );
                    }

                    case "updated_at" -> {
                        if (parser.getValueAsString().equals("updated_at")) continue;

                        fileInfo.setUpdatedAt(
                                LocalDateTime.parse(parser.getValueAsString(), timestampFormatter)
                        );
                    }

                    default -> {}
                }
            }
        }

        return fileInfo;
    }

    private void adaptFileInfoId(FileInfo fileInfo) {
        String id = fileInfo.getId();
        String adaptedId = new IdAdapter(id).getId();

        if (!adaptedId.equals(id)) {
            adaptedFilesIdMap.put(id, adaptedId);
            fileInfo.setId(adaptedId);
        }
    }

    protected final void adaptFileBlobInfoId(FileBlobInfo dataBlock) {
        String id = dataBlock.getId();
        String adaptedId = new IdAdapter(id).getId();

        if (!adaptedId.equals(id)) {
            adaptedDataBlocksIdMap.put(adaptedId, id);
            dataBlock.setId(adaptedId);
        }
    }

    protected abstract void importFilesData()
            throws IOException, DecryptionFailedException, EncryptionFailedException;
}
