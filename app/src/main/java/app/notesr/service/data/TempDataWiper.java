package app.notesr.service.data;

import static app.notesr.util.Wiper.wipeDir;
import static app.notesr.util.Wiper.wipeFile;

import android.util.Log;

import java.io.File;
import java.io.IOException;

public class TempDataWiper {
    private static final String TAG = TempDataWiper.class.getName();

    public static void wipeTempData(File... objects) {
        try {
            for (File object : objects) {
                boolean isWiped = object.isDirectory() ? wipeDir(object) : wipeFile(object);

                if (!isWiped) {
                    throw new IllegalStateException("Temp data was not wiped");
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            throw new RuntimeException(e);
        }
    }
}
