package app.notesr.importer.service.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.exception.ImportFailedException;
import app.notesr.file.model.FileBlobInfo;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.note.model.Note;
import app.notesr.note.service.NoteService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataImporter {
    private static final String NOTES_DIR = "notes/";
    private static final String FILES_INFO_DIR = "finfo/";
    private static final String FILES_BLOBS_INFO_DIR = "binfo/";
    private static final String FILES_BLOBS_DATA_DIR = "fblobs/";

    private final BackupDecryptor decryptor;
    private final NoteService noteService;
    private final FileService fileService;
    private final Path backupZipPath;

    public void importData() throws IOException {
        try (ZipFile zipFile = new ZipFile(backupZipPath.toFile())) {
            importNotes(decryptor, zipFile);
            importFilesInfo(decryptor, zipFile);
            importFilesData(decryptor, zipFile);
        }
    }

    private void importNotes(BackupDecryptor decryptor, ZipFile zipFile) {
        walk(zipFile, NOTES_DIR, entry -> {
            try {
                ObjectMapper objectMapper = getObjectMapper();

                String noteJson = decryptor.decryptJsonObject(readAllBytes(zipFile, entry));
                Note note = objectMapper.readValue(noteJson, Note.class);

                noteService.importNote(note);
            } catch (IOException | DecryptionFailedException e) {
                throw new ImportFailedException(e);
            }
        });
    }

    private void importFilesInfo(BackupDecryptor decryptor, ZipFile zipFile) {
        walk(zipFile, FILES_INFO_DIR, entry -> {
            try {
                ObjectMapper objectMapper = getObjectMapper();

                String fileInfoJson = decryptor.decryptJsonObject(readAllBytes(zipFile, entry));
                FileInfo fileInfo = objectMapper.readValue(fileInfoJson, FileInfo.class);

                fileService.importFileInfo(fileInfo);
            } catch (IOException | DecryptionFailedException e) {
                throw new ImportFailedException(e);
            }
        });
    }

    private void importFilesData(BackupDecryptor decryptor, ZipFile zipFile) {
        walk(zipFile, FILES_BLOBS_INFO_DIR, entry -> {
            try {
                ObjectMapper objectMapper = getObjectMapper();

                String blobInfoJson = decryptor.decryptJsonObject(readAllBytes(zipFile, entry));
                FileBlobInfo blobInfo = objectMapper.readValue(blobInfoJson, FileBlobInfo.class);

                ZipEntry blobDataEntry = zipFile.getEntry(FILES_BLOBS_DATA_DIR
                        + blobInfo.getId());

                if (blobDataEntry == null) {
                    throw new ImportFailedException(
                            new IllegalStateException("Blob data not found for " + blobInfo.getId())
                    );
                }

                byte[] blobData = decryptor.decrypt(readAllBytes(zipFile, blobDataEntry));

                fileService.importFileBlobInfo(blobInfo);
                fileService.importFileBlobData(blobInfo.getId(), blobData);
            } catch (IOException | EncryptionFailedException | DecryptionFailedException e) {
                throw new ImportFailedException(e);
            }
        });
    }

    private void walk(ZipFile zipFile, String dirPrefix, Consumer<ZipEntry> forEachFileAction) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().startsWith(dirPrefix)) {
                forEachFileAction.accept(entry);
            }
        }
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private byte[] readAllBytes(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] tmp = new byte[8192];
            int read;

            while ((read = is.read(tmp)) != -1) {
                buffer.write(tmp, 0, read);
            }

            return buffer.toByteArray();
        }
    }
}
