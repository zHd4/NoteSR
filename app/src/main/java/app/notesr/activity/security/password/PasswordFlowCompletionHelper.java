/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import android.content.Context;
import android.content.Intent;

import app.notesr.BuildConfig;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.FsaResolver;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordFlowCompletionHelper {

    private final AuthActivity activity;
    private final AppSecurityService appSecurityService;
    private final FsaResolver fsaResolver;
    private final AndroidServiceBootstrapper androidServiceBootstrapper;
    private final DataVersionManager dataVersionManager;

    public void bootstrapPostAuthServices(Context context) {
        androidServiceBootstrapper.startServicesPostAuth(context,
                appSecurityService.getActualSecrets());
    }

    public void navigateNextOrIntermediateActivity(Intent targetActivity) {
        Intent intermediateActivity = getIntermediateActivityIntent();
        Intent nextActivity = intermediateActivity == null
                ? targetActivity
                : intermediateActivity;

        navigateNextActivity(nextActivity);
    }

    public void navigateNextActivity(Intent nextActivity) {
        activity.startActivity(nextActivity);
        activity.finish();
    }

    public Intent getIntermediateActivityIntent() {
        if (isMigrationRequired()) {
            return getNewActivityIntent(MigrationActivity.class);
        }

        return getFsaIntent();
    }

    public Intent getFsaIntent() {
        var fsaEntry = fsaResolver.getFsaEntryOfCurrentRunningFs();
        return fsaEntry != null ? getNewActivityIntent(fsaEntry.getActivityClass()) : null;
    }

    public boolean isMigrationRequired() {
        int lastMigrationVersion = dataVersionManager.getCurrentVersion();
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;
        return lastMigrationVersion < currentDataSchemaVersion;
    }

    public Intent getNewActivityIntent(Class<? extends ActivityBase> activityClass) {
        return new Intent(activity.getApplicationContext(), activityClass);
    }
}
