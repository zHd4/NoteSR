/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.List;
import java.util.function.Supplier;

import app.notesr.R;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.lifecycle.AppCloseAndroidService;
import app.notesr.service.lifecycle.AppCloseAndroidServiceStarter;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.activity.security.KeyRecoveryActivity;
import app.notesr.service.security.AppSecurityService;

public final class MainActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyInsets(findViewById(R.id.main));

        var serviceRegistry = AndroidServiceRegistry.getInstance(getApplicationContext());
        var fsaResolver = new FsaResolver(serviceRegistry);

        new AndroidServiceBootstrapper(serviceRegistry)
                .startServicesPreAuth(getApplicationContext());

        var appSecurityService = new AppSecurityService(getApplicationContext());
        var intentSuppliers = getIntentSuppliers(
                getApplicationContext(),
                appSecurityService,
                fsaResolver
        );

        var defaultIntent = new Intent(getApplicationContext(), NotesListActivity.class);

        startAppCloseService(serviceRegistry);
        startActivity(new StartupIntentResolver(intentSuppliers, defaultIntent).resolve());
        finish();
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    private List<Supplier<Intent>> getIntentSuppliers(
            Context context,
            AppSecurityService appSecurityService,
            FsaResolver fsaResolver
    ) {
        return List.of(
                () -> appSecurityService.isAppBlocked()
                        ? new Intent(context, KeyRecoveryActivity.class)
                        : null,

                () -> !appSecurityService.isKeyExists()
                        ? new Intent(context, StartActivity.class)
                        : null,

                () -> !appSecurityService.isAuthConfigured()
                        ? new Intent(context, AuthActivity.class)
                        .putExtra(AuthActivity.EXTRA_MODE,
                                AuthActivity.Mode.AUTHENTICATION.toString())
                        : null,

                () -> {
                    var fsaEntry = fsaResolver.getFsaEntryOfCurrentRunningFs();
                    var activityClass = fsaEntry != null
                            ? fsaEntry.getActivityClass()
                            : null;

                    return activityClass != null
                            ? new Intent(context, activityClass)
                            : null;
                }
        );
    }

    private void startAppCloseService(AndroidServiceRegistry serviceRegistry) {
        if (!serviceRegistry.isServiceRunning(AppCloseAndroidService.class)) {
            new AppCloseAndroidServiceStarter().start(getApplicationContext());
        }
    }
}
