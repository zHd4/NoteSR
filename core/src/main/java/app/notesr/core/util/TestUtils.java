/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.util;

import java.nio.file.Paths;

public final class TestUtils {
    public static String getFixturePath(String pathPart) {
        return Paths.get("src/test/resources/fixtures", pathPart).toString();
    }

    public static String getTempPath(String pathPart) {
        return Paths.get(System.getProperty("java.io.tmpdir"), pathPart).toString();
    }
}
