package app.notesr.service.data;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import app.notesr.utils.Wiper;

public class TempDataWiper {
    private static final String TAG = TempDataWiper.class.getName();

    public static void wipeTempData(File file, File dir) {
        try {
            if (file != null) checkResult(Wiper.wipeFile(file));
            if (dir != null) checkResult(Wiper.wipeDir(dir));
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }

    private static void checkResult(boolean success) {
        if (!success) {
            throw new IllegalStateException("Temp data has not been wiped");
        }
    }
}
