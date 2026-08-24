/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import app.notesr.R;
import app.notesr.core.security.SecretCache;
import app.notesr.core.util.KeyUtils;

@RunWith(AndroidJUnit4.class)
public class ImportKeyActivityTest {

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity();

        SecretCache.clear();
    }

    @After
    public void tearDown() {
        SecretCache.clear();
    }

    @Test
    public void testEmptyKeyImportDoesNothing() {
        try (ActivityScenario<ImportKeyActivity> scenario = ActivityScenario.launch(
                ImportKeyActivity.class)) {
            scenario.onActivity(activity -> {
                EditText keyField = activity.findViewById(R.id.importKeyField);
                Button importKeyButton = activity.findViewById(R.id.importKeyButton);

                keyField.setText("");
                importKeyButton.performClick();

                assertFalse("Empty input should not store a secret",
                        SecretCache.contains(ImportKeyActivity.CACHE_KEY_HEX_KEY));
                assertEquals("Empty input should leave the field blank",
                        "",
                        keyField.getText().toString());
                assertFalse("Empty input should not finish the activity",
                        activity.isFinishing());
                assertEquals("Empty input should leave the activity result canceled",
                        Activity.RESULT_CANCELED,
                        activity.getResultCode());
            });
        }
    }

    @Test
    public void testValidKeyImportStoresSecretAndClearsFieldAndWipesSensitiveData() {
        try (ActivityScenario<ImportKeyActivity> scenario = ActivityScenario.launch(
                ImportKeyActivity.class)) {

            scenario.onActivity(activity -> {
                EditText keyField = activity.findViewById(R.id.importKeyField);
                Button importKeyButton = activity.findViewById(R.id.importKeyButton);

                char[] validKeyHex = buildValidKeyHex();

                keyField.setText(new String(validKeyHex));
                importKeyButton.performClick();

                byte[] expectedKey = KeyUtils.getKeyBytesFromKeyHex(validKeyHex);

                assertTrue("Valid key import should store the secret in the cache",
                        SecretCache.contains(ImportKeyActivity.CACHE_KEY_HEX_KEY));
                assertArrayEquals("Stored key bytes should match the imported key bytes",
                        expectedKey,
                        SecretCache.take(ImportKeyActivity.CACHE_KEY_HEX_KEY));
                assertEquals(
                        "A successful import should clear the entered key from the field",
                        "",
                        keyField.getText().toString());
                assertEquals("A successful import should return RESULT_OK",
                        Activity.RESULT_OK,
                        activity.getResultCode());
                assertTrue("A successful import should finish the activity",
                        activity.isFinishing());

                char[] sensitiveHexKey = activity.getHexKey();
                assertArrayEquals("Sensitive hex input should be wiped on finish",
                        new char[sensitiveHexKey.length],
                        sensitiveHexKey);
            });
        }
    }

    @Test
    public void testInvalidHexImportDoesNotStoreSecretAndKeepsFieldValue() {
        try (ActivityScenario<ImportKeyActivity> scenario = ActivityScenario.launch(
                ImportKeyActivity.class)) {
            scenario.onActivity(activity -> {
                EditText keyField = activity.findViewById(R.id.importKeyField);
                Button importKeyButton = activity.findViewById(R.id.importKeyButton);

                keyField.setText("inv4lidkey");
                importKeyButton.performClick();

                assertFalse("Invalid hex should not be stored in the secret cache",
                        SecretCache.contains(ImportKeyActivity.CACHE_KEY_HEX_KEY));
                assertEquals("Invalid hex should remain visible so the user can correct it",
                        "inv4lidkey",
                        keyField.getText().toString());
                assertFalse("Invalid hex import should not finish the activity",
                        activity.isFinishing());
                assertEquals("Invalid hex import should leave the activity result canceled",
                        Activity.RESULT_CANCELED,
                        activity.getResultCode());
            });
        }
    }

    @Test
    public void testOddLengthImportDoesNotStoreSecretAndKeepsFieldValue() {
        try (ActivityScenario<ImportKeyActivity> scenario = ActivityScenario.launch(
                ImportKeyActivity.class)) {
            scenario.onActivity(activity -> {
                EditText keyField = activity.findViewById(R.id.importKeyField);
                Button importKeyButton = activity.findViewById(R.id.importKeyButton);

                keyField.setText("abc");
                importKeyButton.performClick();

                assertFalse(
                        "Odd-length key input should not be stored in the secret cache",
                        SecretCache.contains(ImportKeyActivity.CACHE_KEY_HEX_KEY));
                assertEquals(
                        "Odd-length key input"
                                + " should remain visible so the user can correct it",
                        "abc",
                        keyField.getText().toString());
                assertFalse(
                        "Odd-length key input should not finish the activity",
                        activity.isFinishing());
                assertEquals(
                        "Odd-length key input should leave the activity result canceled",
                        Activity.RESULT_CANCELED,
                        activity.getResultCode());
            });
        }
    }

    private static char[] buildValidKeyHex() {
        byte[] keyBytes = new byte[48];

        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) i;
        }

        return KeyUtils.getKeyHexFromKeyBytes(keyBytes);
    }
}
