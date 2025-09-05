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
import app.notesr.file.model.DataBlock;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataImporter {
    private static final String NOTES_DIR = "note";
    private static final String FILES_INFOS_DIR = "finfo";
    private static final String DATA_BLOCKS_DIR = "dblock";

    private final BackupDecryptor decryptor;
    private final NoteService noteService;
    private final FileService fileService;
    private final FileSystem backupZipFileSystem;

    public void importData() throws IOException {
        try (backupZipFileSystem) {
            ObjectMapper objectMapper = getObjectMapper();

            importNotes(objectMapper, decryptor, backupZipFileSystem);
            importFilesInfos(objectMapper, decryptor, backupZipFileSystem);
            importDataBlocks(objectMapper, decryptor, backupZipFileSystem);
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

    private void importFilesInfos(ObjectMapper mapper, BackupDecryptor decryptor, FileSystem fs)
            throws IOException {
        Path filesInfosDirPath = fs.getPath("/" + FILES_INFOS_DIR);
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

    private void importDataBlocks(ObjectMapper mapper, BackupDecryptor decryptor, FileSystem fs)
            throws IOException {
        Path dataBlocksDirPath = fs.getPath("/" + DATA_BLOCKS_DIR);
        walk(dataBlocksDirPath, path -> {
            try {
                String dataBlockJson = decryptor.decryptJsonObject(Files.readAllBytes(path));
                DataBlock dataBlock = mapper.readValue(dataBlockJson, DataBlock.class);

                fileService.importDataBlock(dataBlock);
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
