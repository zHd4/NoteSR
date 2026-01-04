/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import java.io.File;
import java.io.IOException;

public final class TempDataWiper {

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
