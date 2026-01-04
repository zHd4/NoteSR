/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import java.io.File;
import java.io.IOException;

public interface WiperAdapter {
    void wipeDir(File dir) throws IOException;
    void wipeFile(File file) throws IOException;
}
