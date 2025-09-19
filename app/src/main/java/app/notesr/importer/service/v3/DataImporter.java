package app.notesr.importer.service.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.ImportFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataImporter {
    private static final String NOTES_DIR = "notes";
    private static final String FILES_INFO_DIR = "finfo";
    private static final String FILES_BLOBS_INFO_DIR = "binfo";
    private static final String FILES_BLOBS_DATA_DIR = "fblobs";

    private final BackupDecryptor decryptor;
    private final NoteService noteService;
    private final FileService fileService;
    private final FileSystem backupZipFileSystem;

    public void importData() throws IOException {
        try (backupZipFileSystem) {
            ObjectMapper objectMapper = getObjectMapper();

            importNotes(objectMapper, decryptor, backupZipFileSystem);
            importFilesInfo(objectMapper, decryptor, backupZipFileSystem);
            importFilesData(objectMapper, decryptor, backupZipFileSystem);
        }
    }

    private void importNotes(ObjectMapper mapper, BackupDecryptor decryptor, FileSystem fs)
            throws IOException {
        Path notesDirPath = fs.getPath("/" + NOTES_DIR);
        walk(notesDirPath, path -> {
            try {
                String noteJson = decryptor.decryptJsonObject(Files.readAllBytes(path));
                Note note = mapper.readValue(noteJson, Note.class);

                noteService.importNote(note);
            } catch (IOException | DecryptionFailedException e) {
                throw new ImportFailedException(e);
            }
        });
    }

    private void importFilesInfo(ObjectMapper mapper, BackupDecryptor decryptor, FileSystem fs)
            throws IOException {
        Path filesInfosDirPath = fs.getPath("/" + FILES_INFO_DIR);
        walk(filesInfosDirPath, path -> {
            try {
                String fileInfoJson = decryptor.decryptJsonObject(Files.readAllBytes(path));
                FileInfo fileInfo = mapper.readValue(fileInfoJson, FileInfo.class);

                fileService.importFileInfo(fileInfo);
            } catch (IOException | DecryptionFailedException e) {
                throw new ImportFailedException(e);
            }
        });
    }

    private void importFilesData(ObjectMapper mapper, BackupDecryptor decryptor, FileSystem fs)
            throws IOException {
        Path blobsInfoDirPath = fs.getPath("/" + FILES_BLOBS_INFO_DIR);
        Path blobsDataDirPath = fs.getPath("/" + FILES_BLOBS_DATA_DIR);

        walk(blobsInfoDirPath, path -> {
            try {
                String blobInfoJson = decryptor.decryptJsonObject(Files.readAllBytes(path));
                FileBlobInfo blobInfo = mapper.readValue(blobInfoJson, FileBlobInfo.class);

                Path blobDataPath = blobsDataDirPath.resolve(blobInfo.getId());
                byte[] blobData = decryptor.decrypt(Files.readAllBytes(blobDataPath));

                fileService.importFileBlobInfo(blobInfo);
                fileService.importFileBlobData(blobInfo.getId(), blobData);
            } catch (IOException | DecryptionFailedException e) {
                throw new ImportFailedException(e);
            }
        });
    }

    private void walk(Path dirPath, Consumer<Path> forEachFileAction) throws IOException {
        try (var paths = Files.walk(dirPath)) {
            paths.filter(Files::isRegularFile).forEach(forEachFileAction);
        }
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
