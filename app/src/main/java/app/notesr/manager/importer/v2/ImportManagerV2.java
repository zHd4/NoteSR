package app.notesr.manager.importer.v2;

import static java.util.UUID.randomUUID;

import android.content.Context;
import java.io.File;
import java.io.IOException;

import app.notesr.manager.importer.BaseImportManager;
import app.notesr.manager.importer.ImportResult;
import app.notesr.utils.ZipUtils;
import lombok.Getter;

public class ImportManagerV2 extends BaseImportManager {

    @Getter
    private ImportResult result = ImportResult.NONE;

    @Getter
    private String status = "";

    private File tempDir;

    public ImportManagerV2(Context context, File file) {
        super(context, file);
    }

    @Override
    public void start() {
        Thread thread = new Thread(() -> {
            try {
                tempDir = new File(context.getCacheDir(), randomUUID().toString());
                ZipUtils.unzip(file.getAbsolutePath(), tempDir.getAbsolutePath(), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
    }
}
