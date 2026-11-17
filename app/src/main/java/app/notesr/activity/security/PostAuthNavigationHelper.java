/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.FsaResolver;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostAuthNavigationHelper {

    private final AuthActivity activity;
    private final AppSecurityService appSecurityService;
    private final FsaResolver fsaResolver;
    private final AndroidServiceBootstrapper androidServiceBootstrapper;
    private final DataVersionManager dataVersionManager;

    public void clearPasswordAndBootstrap(Context context) {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);

        censoredPasswordView.setText("");
        androidServiceBootstrapper.startServicesPostAuth(context,
                appSecurityService.getActualSecrets());
    }

    public Intent getRequiredIntent() {
        Intent fsaIntent = getFsaIntent();

        if (fsaIntent != null) {
            return fsaIntent;
        }

        if (isMigrationRequired()) {
            return getNewIntent(MigrationActivity.class);
        }

        return null;
    }

    public Intent getNewIntent(Class<? extends ActivityBase> activityClass) {
        return new Intent(activity.getApplicationContext(), activityClass);
    }

    public Intent getFsaIntent() {
        var fsaEntry = fsaResolver.getFsaEntryOfCurrentRunningFs();
        return fsaEntry != null ? getNewIntent(fsaEntry.getActivityClass()) : null;
    }

    public boolean isMigrationRequired() {
        int lastMigrationVersion = dataVersionManager.getCurrentVersion();
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;
        return lastMigrationVersion < currentDataSchemaVersion;
    }

    public void navigateAfterAuth() {
        Intent requiredIntent = getRequiredIntent();
        Intent nextIntent = requiredIntent == null
                ? getNewIntent(NotesListActivity.class)
                : requiredIntent;

        activity.startActivity(nextIntent);
        activity.finish();
    }
}
