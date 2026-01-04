/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import app.notesr.core.util.VersionFetcher;

public final class VersionFetcherImpl implements VersionFetcher {
    @Override
    public String fetchVersionName(Context context, boolean removeDot)
            throws PackageManager.NameNotFoundException {

        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

        return removeDot
                ? packageInfo.versionName.replace(".", "")
                : packageInfo.versionName;
    }
}
