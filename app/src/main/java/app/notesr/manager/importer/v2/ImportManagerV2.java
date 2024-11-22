package app.notesr.manager.importer.v2;

import static java.util.UUID.randomUUID;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

import app.notesr.R;
import app.notesr.manager.importer.BaseImportManager;
import app.notesr.manager.importer.ImportResult;
import app.notesr.utils.Wiper;
import app.notesr.utils.ZipUtils;
import lombok.Getter;

public class ImportManagerV2 extends BaseImportManager {

    private static final String TAG = ImportManagerV2.class.getName();

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

                status = context.getString(R.string.importing);
                ZipUtils.unzip(file.getAbsolutePath(), tempDir.getAbsolutePath(), null);

                status = context.getString(R.string.wiping_temp_data);
                wipeTempData();

                result = ImportResult.FINISHED_SUCCESSFULLY;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        thread.start();
    }

    private void wipeTempData() {
        try {
            if (!Wiper.wipeAny(List.of(file, tempDir))) {
                throw new IllegalStateException("Temp data has not been wiped");
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }
}
