package app.notesr.file.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.file.model.FileInfo;
import app.notesr.file.service.FileService;
import app.notesr.util.FilesUtilsAdapter;
import app.notesr.util.HashUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class FileIOHelper {
    private final FilesUtilsAdapter filesUtils;
    private final FileService fileService;

    public void exportFile(String fileId, File destFile) {
        try {
            fileService.read(fileId, chunk -> {
                try {
                    filesUtils.writeFileBytes(destFile, chunk, true);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write file", e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        } catch (DecryptionFailedException e) {
            throw new RuntimeException("Failed to decrypt file", e);
        }
    }

    public File dropToCache(FileInfo fileInfo, File cacheDir) {
        try {
            String name = generateTempName(fileInfo);
            String ext = filesUtils.getFileExtension(fileInfo.getName());
            File tempFile = File.createTempFile(name, "." + ext, cacheDir);

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                fileService.read(fileInfo.getId(), chunk -> {
                    try {
                        out.write(chunk);
                    } catch (IOException e) {
                        throw new RuntimeException("Writing to cache failed", e);
                    }
                });
            }

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to cache file", e);
        } catch (DecryptionFailedException e) {
            throw new RuntimeException("Failed to decrypt file", e);
        }
    }

    private String generateTempName(FileInfo info) {
        try {
            return HashUtils.toSha256String(info.getId() + "$" + info.getName());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
