package app.notesr.util;

import java.io.File;
import java.io.IOException;

public class TempDataWiper {

    public static void wipeTempData(File... objects) throws IOException {
        Wiper wiper = new Wiper();

        for (File object : objects) {
            if (object != null && object.exists()) {
                if (object.isDirectory()) {
                    wiper.wipeDir(object);
                } else {
                    wiper.wipeFile(object);
                }
            }
        }
    }
}
