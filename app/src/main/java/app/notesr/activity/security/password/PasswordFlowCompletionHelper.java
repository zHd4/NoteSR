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

/**
 * Completes the authenticated password flow by starting post-authentication
 * services and routing to the next required activity.
 *
 * <p>Data migration takes precedence over foreground service actions; otherwise, the
 * target or intermediate activity is used when neither is required.
 */
@RequiredArgsConstructor
public class PasswordFlowCompletionHelper {

    private final AppPasswordActivity activity;
    private final AppSecurityService appSecurityService;
    private final FsaResolver fsaResolver;
    private final AndroidServiceBootstrapper androidServiceBootstrapper;
    private final DataVersionManager dataVersionManager;

    /**
     * Starts services that require authentication using the current secrets.
     *
     * @param context context used to start the services
     */
    public void bootstrapPostAuthServices(Context context) {
        androidServiceBootstrapper.startServicesPostAuth(context,
                appSecurityService.getActualSecrets());
    }

    /**
     * Navigates to a required migration or foreground service activity, falling back
     * to the supplied target when no intermediate activity is needed.
     *
     * @param targetActivity activity to open when no intermediate activity is
     *                       required
     */
    public void navigateNextOrIntermediateActivity(Intent targetActivity) {
        Intent intermediateActivity = getIntermediateActivityIntent();
        Intent nextActivity = intermediateActivity == null
                ? targetActivity
                : intermediateActivity;

        navigateNextActivity(nextActivity);
    }

    /**
     * Opens the given activity and finishes the current password activity.
     *
     * @param nextActivity intent for the activity to open
     */
    public void navigateNextActivity(Intent nextActivity) {
        activity.startActivity(nextActivity);
        activity.finish();
    }

    /**
     * Gets the activity required before the requested destination, if any.
     *
     * @return an intent for the migration activity or a foreground service activity,
     *         or {@code null} if neither is required
     */
    public Intent getIntermediateActivityIntent() {
        if (isMigrationRequired()) {
            return getNewActivityIntent(MigrationActivity.class);
        }

        return getFsaIntent();
    }

    /**
     * Gets the foreground service activity associated with the current file system
     * adapter.
     *
     * @return an intent for the associated activity, or {@code null} if the
     *         current adapter has no foreground service activity
     */
    public Intent getFsaIntent() {
        var fsaEntry = fsaResolver.getFsaEntryOfCurrentRunningFs();
        return fsaEntry != null ? getNewActivityIntent(fsaEntry.getActivityClass()) : null;
    }

    /**
     * Checks whether the stored data schema predates the current schema.
     *
     * @return {@code true} if a data migration is required
     */
    public boolean isMigrationRequired() {
        int lastMigrationVersion = dataVersionManager.getCurrentVersion();
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;
        return lastMigrationVersion < currentDataSchemaVersion;
    }

    /**
     * Creates an intent for an activity using this activity's application
     * context.
     *
     * @param activityClass class of the activity to open
     * @return an intent targeting {@code activityClass}
     */
    public Intent getNewActivityIntent(Class<? extends ActivityBase> activityClass) {
        return new Intent(activity.getApplicationContext(), activityClass);
    }
}
