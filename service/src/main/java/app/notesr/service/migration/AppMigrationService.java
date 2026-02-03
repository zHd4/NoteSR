/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration;

import android.content.Context;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AppMigrationService {
    private final List<AppMigration> migrations;

    public void run(Context context, int oldVersion, int newVersion) throws AppMigrationException {
        for (AppMigration migration : migrations) {
            if (migration.getFromVersion() >= oldVersion
                    && migration.getToVersion() <= newVersion) {
                migration.migrate(context);
            }
        }
    }
}
