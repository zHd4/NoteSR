package app.notesr.service.cachecleaner;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import app.notesr.core.util.Wiper;
import app.notesr.data.model.TempFile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CacheCleanerService {

    private static final String TAG = CacheCleanerService.class.getCanonicalName();

    private final TempFileService tempFileService;
    private final Map<TempFile, Thread> runningJobs = new LinkedHashMap<>();

    public void cleanupTempFilesAsync() {
        tempFileService.getAll().stream()
                .filter(tempFile -> !runningJobs.containsKey(tempFile))
                .forEach(tempFile -> {
                    Thread deletionJob = new Thread(createFileDeletionJob(tempFile));
                    runningJobs.put(tempFile, deletionJob);
                    deletionJob.start();
                });
    }

    public boolean hasRunningJobs() {
        return !runningJobs.isEmpty();
    }

    private Runnable createFileDeletionJob(TempFile tempFile) {
        return () -> {
            File file = new File(Objects.requireNonNull(tempFile.getUri().getPath()));

            if (file.exists()) {
                try {
                    new Wiper().wipeFile(file);
                } catch (IOException e) {
                    Log.e(TAG, "Cannot wipe file", e);
                }
            }

            tempFileService.delete(tempFile.getId());
            runningJobs.remove(tempFile);
        };
    }
}
