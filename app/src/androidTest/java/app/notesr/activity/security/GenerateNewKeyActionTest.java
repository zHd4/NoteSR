/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import app.notesr.activity.ActivityBase;

@RunWith(AndroidJUnit4.class)
public class GenerateNewKeyActionTest {

    private Context context;
    private ActivityBase activity;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        activity = mock(ActivityBase.class);
    }

    @Test
    public void testGenerateNewKeyStartsSetupKeyActivity() {
        when(activity.getApplicationContext()).thenReturn(context);

        GenerateNewKeyAction action = new GenerateNewKeyAction(activity);
        action.startActivity();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentCaptor.capture());
        Intent startedIntent = intentCaptor.getValue();

        assertNotNull("Intent should be created", startedIntent);
        assertNotNull("Intent component should be set", startedIntent.getComponent());
        assertEquals("Intent component should target SetupKeyActivity",
                SetupKeyActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    @Test
    public void testGenerateNewKeyStartsSetupKeyActivityWithRegenerationMode() {
        when(activity.getApplicationContext()).thenReturn(context);

        GenerateNewKeyAction action = new GenerateNewKeyAction(activity);
        action.startActivity();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(activity).startActivity(intentCaptor.capture());
        Intent startedIntent = intentCaptor.getValue();

        assertNotNull("Intent should be created", startedIntent);
        assertEquals("Intent should use regeneration mode",
                KeySetupMode.REGENERATION.getModeName(),
                startedIntent.getStringExtra(SetupKeyActivity.EXTRA_MODE));
        verify(activity).startActivity(any(Intent.class));
    }
}
