/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration;

import android.content.Context;

public interface AppMigration {
    int getFromVersion();

    int getToVersion();

    void migrate(Context context);
}
