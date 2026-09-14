/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.service.security.rotation.SecretsUpdateAndroidService;
import app.notesr.core.security.SecretCache;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.AndroidServiceEntry;
import app.notesr.service.security.rotation.SecretsUpdateAndroidServiceStarter;

@RunWith(AndroidJUnit4.class)
public class SecretsUpdateActivityTest {

    private Context context;
    private Instrumentation instrumentation;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();

        instrumentation.getUiAutomation().adoptShellPermissionIdentity();

        AndroidServiceRegistry registry = AndroidServiceRegistry.getInstance(context);
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceName(SecretsUpdateAndroidService.class.getSimpleName())
                .serviceClass(SecretsUpdateAndroidService.class)
                .starterClass(SecretsUpdateAndroidServiceStarter.class)
                .autoStart(true)
                .requiresAuth(true)
                .build();

        registry.register(entry);
    }

    @After
    public void tearDown() {
        AndroidServiceRegistry.getInstance(context).unregister(SecretsUpdateAndroidService.class);
        SecretCache.clear();
    }

    @Test
    public void secretsUpdateCompleteNavigatesToNotesListAndFinishes() {
        ActivityScenario<SecretsUpdateActivity> scenario =
                ActivityScenario.launch(SecretsUpdateActivity.class);

        try (scenario) {
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                    NotesListActivity.class.getName(), null, false);

            Intent intent = new Intent(SecretsUpdateAndroidService.BROADCAST_ACTION);
            intent.putExtra(SecretsUpdateAndroidService.EXTRA_COMPLETE, true);

            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(intent);

            Activity started = instrumentation.waitForMonitorWithTimeout(monitor, 2000);
            instrumentation.removeMonitor(monitor);

            assertNotNull("NotesListActivity should be started on secrets update complete",
                    started);
        }
    }

    @Test
    public void secretsUpdateFailedNavigatesToNotesListActivity() {
        ActivityScenario<SecretsUpdateActivity> scenario =
                ActivityScenario.launch(SecretsUpdateActivity.class);

        try (scenario) {
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                    NotesListActivity.class.getName(), null, false);

            Intent intent = new Intent(SecretsUpdateAndroidService.BROADCAST_ACTION);
            intent.putExtra(SecretsUpdateAndroidService.EXTRA_FAIL, true);

            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            Activity started = instrumentation.waitForMonitorWithTimeout(monitor, 2000);
            instrumentation.removeMonitor(monitor);

            assertNotNull("NotesListActivity should be started when secrets update fails",
                    started);
        }
    }
}
