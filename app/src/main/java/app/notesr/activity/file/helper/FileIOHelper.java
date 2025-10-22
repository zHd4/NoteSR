package app.notesr.activity.file.helper;

import java.io.File;
import java.io.IOException;

import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.util.FilesUtilsAdapter;
import app.notesr.service.file.FileService;
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
}
