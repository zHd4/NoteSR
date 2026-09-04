/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.Instrumentation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import app.notesr.R;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.KeyUtils;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import app.notesr.util.ActivityUtils;
import io.bloco.faker.Faker;

@RunWith(AndroidJUnit4.class)
public class KeyRecoveryActivityTest {

    private static final Faker FAKER = new Faker();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Instrumentation instrumentation;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity();
        SecretCache.clear();
    }

    @After
    public void tearDown() {
        SecretCache.clear();
    }

    @Test
    public void testActivityInitializesRecoveryFieldAndApplyButton() {
        try (ActivityScenario<KeyRecoveryActivity> scenario = ActivityScenario.launch(
                KeyRecoveryActivity.class)) {
            scenario.onActivity(activity -> {
                EditText recoveryKeyField = activity.findViewById(R.id.importRecoveryKeyField);
                Button applyButton = activity.findViewById(R.id.applyRecoveryKeyButton);

                assertNotNull("Recovery key input should be attached to the layout",
                        recoveryKeyField);
                assertNotNull("Apply button should be attached to the layout",
                        applyButton);

                ActionBar actionBar = activity.getSupportActionBar();
                assertNotNull("Action bar should exist on the key recovery screen",
                        actionBar);

                CharSequence actionBarTitle = actionBar.getTitle();
                assertNotNull(
                        "Action bar title should be present on the key recovery screen",
                        actionBarTitle);
                assertEquals("Key recovery screen title should match the recovery label",
                        activity.getString(R.string.key_recovery),
                        actionBarTitle.toString());
            });
        }
    }

    @Test
    public void testEmptyRecoveryKeyDoesNothing() {
        try (ActivityScenario<KeyRecoveryActivity> scenario = ActivityScenario.launch(
                KeyRecoveryActivity.class)) {
            scenario.onActivity(activity -> {
                AppSecurityService spyAppSecurityService = spy(activity.getAppSecurityService());
                activity.setAppSecurityService(spyAppSecurityService);
                spyAppSecurityService.setSecrets(getTestCryptoSecrets());

                EditText recoveryKeyField = activity.findViewById(R.id.importRecoveryKeyField);
                Button applyButton = activity.findViewById(R.id.applyRecoveryKeyButton);

                recoveryKeyField.setText("");
                applyButton.performClick();

                assertFalse("Empty recovery input should not write a key to the cache",
                        SecretCache.contains(AuthActivity.CACHE_KEY_HEX_KEY));
                assertEquals("Empty recovery input should leave the field blank",
                        "",
                        recoveryKeyField.getText().toString());
                assertFalse("Empty recovery input should not finish the activity",
                        activity.isFinishing());
            });
        }
    }

    @Test
    public void testValidRecoveryKeyStoresSecretAndStartsAuthActivity() {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                AuthActivity.class.getName(), null, false);

        try (ActivityScenario<KeyRecoveryActivity> scenario = ActivityScenario.launch(
                KeyRecoveryActivity.class)) {
            scenario.onActivity(activity -> {
                CryptoSecrets testSecrets = getTestCryptoSecrets();
                AppSecurityService spyAppSecurityService = spy(activity.getAppSecurityService());
                activity.setAppSecurityService(spyAppSecurityService);

                spyAppSecurityService.setSecrets(testSecrets);
                doReturn(true).when(spyAppSecurityService)
                        .isKeyMatchingWithStored(any(byte[].class));

                EditText recoveryKeyField = activity.findViewById(R.id.importRecoveryKeyField);
                Button applyButton = activity.findViewById(R.id.applyRecoveryKeyButton);
                char[] validKeyHex = KeyUtils.getKeyHexFromKeyBytes(testSecrets.getKey());
                byte[] expectedCacheValue = new String(validKeyHex)
                        .getBytes(StandardCharsets.UTF_8);

                recoveryKeyField.setText(new String(validKeyHex));
                applyButton.performClick();

                assertTrue("A valid recovery key should be cached before auth starts",
                        SecretCache.contains(AuthActivity.CACHE_KEY_HEX_KEY));
                assertArrayEquals(
                        "The cached recovery key should contain the hex characters",
                        expectedCacheValue,
                        SecretCache.take(AuthActivity.CACHE_KEY_HEX_KEY));
                assertEquals(
                        "A successful recovery should clear the entered key from the field",
                        "",
                        recoveryKeyField.getText().toString());
            });

            Activity started = instrumentation.waitForMonitorWithTimeout(monitor, 20000);

            assertNotNull(
                    "AuthActivity should be started after a valid recovery key is accepted",
                    started);
            assertEquals(
                    "Recovered key flow should start AuthActivity in key recovery mode",
                    AuthActivity.Mode.KEY_RECOVERY.getModeName(),
                    started.getIntent().getStringExtra(AuthActivity.EXTRA_MODE));

            started.finish();
        }
    }

    @Test
    public void testWrongRecoveryKeyDoesNotFinishActivity() {
        try (ActivityScenario<KeyRecoveryActivity> scenario = ActivityScenario.launch(
                KeyRecoveryActivity.class)) {
            scenario.onActivity(activity -> {
                CryptoSecrets testSecrets = getTestCryptoSecrets();
                AppSecurityService spyAppSecurityService = spy(activity.getAppSecurityService());
                activity.setAppSecurityService(spyAppSecurityService);

                spyAppSecurityService.setSecrets(testSecrets);
                doReturn(false).when(spyAppSecurityService)
                        .isKeyMatchingWithStored(any(byte[].class));

                ActivityUtils activityUtils = spy(activity.getActivityUtils());
                activity.setActivityUtils(activityUtils);

                EditText recoveryKeyField = activity.findViewById(R.id.importRecoveryKeyField);
                Button applyButton = activity.findViewById(R.id.applyRecoveryKeyButton);

                char[] wrongKeyHex = KeyUtils.getKeyHexFromKeyBytes(
                        incrementBytes(testSecrets.getKey()));

                recoveryKeyField.setText(new String(wrongKeyHex));
                applyButton.performClick();

                assertFalse("Wrong recovery keys should not store anything in the cache",
                        SecretCache.contains(AuthActivity.CACHE_KEY_HEX_KEY));
                assertEquals(
                        "Wrong recovery keys should be left in the field for correction",
                        new String(wrongKeyHex),
                        recoveryKeyField.getText().toString());
                assertFalse("Wrong recovery keys should not finish the current activity",
                        activity.isFinishing());

                verify(activityUtils).showToastMessage(
                        "Wrong key!",
                        Toast.LENGTH_SHORT);
            });
        }
    }

    @Test
    public void testInvalidRecoveryKeyDoesNotFinishActivity() {
        try (ActivityScenario<KeyRecoveryActivity> scenario = ActivityScenario.launch(
                KeyRecoveryActivity.class)) {
            scenario.onActivity(activity -> {
                AppSecurityService spyAppSecurityService = spy(activity.getAppSecurityService());
                activity.setAppSecurityService(spyAppSecurityService);
                spyAppSecurityService.setSecrets(getTestCryptoSecrets());

                ActivityUtils activityUtils = spy(activity.getActivityUtils());
                activity.setActivityUtils(activityUtils);

                EditText recoveryKeyField = activity.findViewById(R.id.importRecoveryKeyField);
                Button applyButton = activity.findViewById(R.id.applyRecoveryKeyButton);
                String invalidKey = "inv4lidkey";

                recoveryKeyField.setText(invalidKey);
                applyButton.performClick();

                assertFalse(
                        "Invalid recovery key input should not be stored in the cache",
                        SecretCache.contains(AuthActivity.CACHE_KEY_HEX_KEY));
                assertEquals("Invalid recovery keys should stay visible"
                                + " so the user can fix them",
                        invalidKey,
                        recoveryKeyField.getText().toString());
                assertFalse("Invalid recovery key input should not finish the activity",
                        activity.isFinishing());

                verify(activityUtils).showToastMessage(
                        "Invalid key!",
                        Toast.LENGTH_SHORT);
            });
        }
    }

    @Test
    public void testKeyMatchingFailureThrowsRuntimeExceptionWithCause() {
        try (ActivityScenario<KeyRecoveryActivity> scenario = ActivityScenario.launch(
                KeyRecoveryActivity.class)) {
            scenario.onActivity(activity -> {
                CryptoSecrets testSecrets = getTestCryptoSecrets();
                AppSecurityService spyAppSecurityService = spy(activity.getAppSecurityService());
                activity.setAppSecurityService(spyAppSecurityService);

                doThrow(new AppSecurityException("Key matching failed"))
                        .when(spyAppSecurityService)
                        .isKeyMatchingWithStored(any(byte[].class));

                EditText recoveryKeyField = activity.findViewById(R.id.importRecoveryKeyField);
                Button applyButton = activity.findViewById(R.id.applyRecoveryKeyButton);
                char[] validHex = KeyUtils.getKeyHexFromKeyBytes(testSecrets.getKey());

                recoveryKeyField.setText(new String(validHex));

                try {
                    applyButton.performClick();
                    fail("Expected RuntimeException to be thrown when AppSecurityException occurs");
                } catch (RuntimeException e) {
                    assertTrue("Cause should be AppSecurityException",
                            e.getCause() instanceof AppSecurityException);
                }
            });
        }
    }

    private static byte[] getTestKeyBytes() {
        byte[] keyBytes = new byte[48];
        SECURE_RANDOM.nextBytes(keyBytes);
        return keyBytes;
    }

    private static CryptoSecrets getTestCryptoSecrets() {
        return new CryptoSecrets(getTestKeyBytes(), FAKER.internet.password().toCharArray());
    }

    private static byte[] incrementBytes(byte[] bytes) {
        byte[] newBytes = new byte[bytes.length];

        for (int i = 0; i < newBytes.length; i++) {
            newBytes[i] = (byte) ((bytes[i] + 1) & 0xFF);
        }

        return newBytes;
    }
}
