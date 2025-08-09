package app.notesr.util;

import static app.notesr.util.Wiper.wipeDir;
import static app.notesr.util.Wiper.wipeFile;

import java.io.File;
import java.io.IOException;

public class TempDataWiper {

    public static void wipeTempData(File... objects) throws IOException {
        for (File object : objects) {
            if (object != null) {
                boolean isWiped = object.isDirectory() ? wipeDir(object) : wipeFile(object);

                if (!isWiped) {
                    throw new IllegalStateException("Temp data was not wiped");
                }
            }
        }
    }
}
