/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import java.util.Set;

import app.notesr.activity.exporter.ExportActivity;
import app.notesr.activity.importer.ImportActivity;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.activity.security.ReEncryptionActivity;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.exporter.ExportAndroidService;
import app.notesr.service.importer.ImportAndroidService;
import app.notesr.service.migration.AppMigrationAndroidService;
import app.notesr.service.security.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

/**
 * Resolver for Foreground Service Association (FSA).
 * <p>
 * This class maps background {@link android.app.Service} classes to their
 * corresponding {@link android.app.Activity} classes. It is used to determine
 * which UI component should be displayed based on the currently running
 * background process.
 */
@RequiredArgsConstructor
public final class FsaResolver {

    /**
     * Internal registry of associations between services and activities.
     */
    private static final Set<FsaEntry> fsaRegistry = Set.of(
            new FsaEntry(AppMigrationAndroidService.class, MigrationActivity.class),
            new FsaEntry(ExportAndroidService.class, ExportActivity.class),
            new FsaEntry(ImportAndroidService.class, ImportActivity.class),
            new FsaEntry(SecretsUpdateAndroidService.class, ReEncryptionActivity.class)
    );

    private final AndroidServiceRegistry servicesRegistry;

    /**
     * Identifies the {@link FsaEntry} corresponding to the foreground service
     * that is currently active.
     *
     * @return the matching {@link FsaEntry} if a registered service is running,
     *         or {@code null} if no registered services are active.
     */
    public FsaEntry getFsaEntryOfCurrentRunningFs() {
        return fsaRegistry.stream()
                .filter(entry ->
                        servicesRegistry.isServiceRunning(entry.getForegroundServiceClass()))
                .findFirst()
                .orElse(null);
    }
}
