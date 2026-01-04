/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.util;

import android.content.Context;
import android.content.pm.PackageManager;

public interface VersionFetcher {
    String fetchVersionName(Context context, boolean removeDot) throws
            PackageManager.NameNotFoundException;
}
