/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Objects.requireNonNull;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import app.notesr.R;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.di.service.security.AppSecurityServiceModule;
import app.notesr.service.security.AppSecurityService;
import dagger.hilt.android.testing.BindValue;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;

@RunWith(AndroidJUnit4.class)
@HiltAndroidTest
@UninstallModules(AppSecurityServiceModule.class)
public class AppPasswordActivityTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @BindValue
    AppSecurityService appSecurityService = mock(AppSecurityService.class);

    private Context context;

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentation.getTargetContext();

        when(appSecurityService.isKeyExists()).thenReturn(false);
        hiltRule.inject();
    }

    private void enterPasswordUsingKeyboard(AppPasswordActivity activity, String password) {
        LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);

        for (char character : password.toCharArray()) {
            String keyText = String.valueOf(character);
            boolean keyFound = false;

            for (int rowIndex = 0; rowIndex < keyboardContainer.getChildCount(); rowIndex++) {
                LinearLayout row = (LinearLayout) keyboardContainer.getChildAt(rowIndex);

                for (int keyIndex = 0; keyIndex < row.getChildCount(); keyIndex++) {
                    Button key = (Button) row.getChildAt(keyIndex);
                    if (keyText.equals(key.getText().toString())) {
                        key.performClick();
                        keyFound = true;
                        break;
                    }
                }

                if (keyFound) {
                    break;
                }
            }

            assertTrue("Keyboard should contain the test character " + character, keyFound);
        }
    }

    @Test
    public void testModeFromStringMatchesKnownModes() {
        assertEquals("Authentication mode should map to the authentication constant",
                AppPasswordActivity.Mode.AUTHENTICATION,
                AppPasswordActivity.Mode.fromString(AppPasswordActivity.Mode.AUTHENTICATION.getModeName()));
        assertEquals("Create password mode should map to the create password constant",
                AppPasswordActivity.Mode.CREATE_PASSWORD,
                AppPasswordActivity.Mode.fromString(AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName()));
        assertEquals("Change password mode should map to the change password constant",
                AppPasswordActivity.Mode.CHANGE_PASSWORD,
                AppPasswordActivity.Mode.fromString(AppPasswordActivity.Mode.CHANGE_PASSWORD.getModeName()));
        assertEquals("Key recovery mode should map to the key recovery constant",
                AppPasswordActivity.Mode.KEY_RECOVERY,
                AppPasswordActivity.Mode.fromString(AppPasswordActivity.Mode.KEY_RECOVERY.getModeName()));
    }

    @Test
    public void testModeFromStringThrowsForUnknownMode() {
        IllegalArgumentException exception = assertThrows(
                "Unknown modes should throw an exception",
                IllegalArgumentException.class,
                () -> AppPasswordActivity.Mode.fromString("invalid"));

        assertNotNull("Exception message should not be null", exception.getMessage());
        assertTrue("The exception message should include the invalid value",
                requireNonNull(exception.getMessage()).contains("invalid"));
    }

    @Test
    public void testModeFromStringThrowsForNullMode() {
        IllegalArgumentException exception = assertThrows(
                "Missing or null modes should be rejected consistently",
                IllegalArgumentException.class,
                () -> AppPasswordActivity.Mode.fromString(null));

        assertNotNull("Null-mode exception message should not be null",
                requireNonNull(exception.getMessage()));
        assertTrue("The null-mode exception message should mention the null value",
                requireNonNull(exception.getMessage()).contains("null"));
    }

    @Test
    public void testInvalidModeFinishesActivityGracefully() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, "invalid");

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Activity should finish when its mode is invalid",
                    Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void testKeyboardStartsWithAlphaNumericLayout() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);

                assertNotNull(
                        "Keyboard container should be initialized during activity creation",
                        keyboardContainer);
                assertEquals(
                        "The default keyboard should render four rows",
                        4,
                        keyboardContainer.getChildCount());

                LinearLayout firstRow = (LinearLayout) keyboardContainer.getChildAt(0);
                assertEquals(
                        "The first alpha numeric row should contain ten keys",
                        10,
                        firstRow.getChildCount());

                Button firstKey = (Button) firstRow.getChildAt(0);
                assertEquals(
                        "The first key should display the digit one",
                        "1",
                        firstKey.getText().toString());
            });
        }
    }

    @Test
    public void testAlphaNumericKeyboardShowsExactRows() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
                String[][] expectedRows = {
                        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
                        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
                        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
                        {"z", "x", "c", "v", "b", "n", "m"}
                };

                assertEquals("Alpha numeric layout should render four rows",
                        4, keyboardContainer.getChildCount());

                for (int rowIndex = 0; rowIndex < expectedRows.length; rowIndex++) {
                    LinearLayout row = (LinearLayout) keyboardContainer.getChildAt(rowIndex);
                    assertEquals("Row " + rowIndex + " should have the exact key count",
                            expectedRows[rowIndex].length, row.getChildCount());

                    for (int keyIndex = 0; keyIndex < expectedRows[rowIndex].length;
                         keyIndex++) {
                        Button key = (Button) row.getChildAt(keyIndex);
                        assertEquals("Unexpected key at row " + rowIndex + ", index " + keyIndex,
                                expectedRows[rowIndex][keyIndex],
                                key.getText().toString());
                    }
                }
            });
        }
    }

    @Test
    public void testCapsToggleAndBackspaceRemoveCharacters() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);
                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
                Button firstKey = (Button) ((LinearLayout) keyboardContainer.getChildAt(0))
                        .getChildAt(0);

                firstKey.performClick();

                assertEquals("The password field should show one bullet after a key press",
                        "•",
                        passwordView.getText().toString());
                assertEquals("The password builder"
                                + " should contain one character after a key press",
                        1,
                        activity.getPasswordBuilder().length());

                Button capsButton = activity.findViewById(R.id.capsButton);
                capsButton.performClick();

                assertTrue("Caps lock should be enabled after pressing the caps button",
                        activity.isCapsLockEnabled());

                Button changeLayoutButton = activity.findViewById(R.id.changeKeyboardLayoutButton);
                changeLayoutButton.performClick();

                assertTrue("The symbol keyboard should be active after toggling the layout",
                        activity.isShowingSymbols());
                assertEquals("The symbol keyboard should render three rows",
                        3,
                        keyboardContainer.getChildCount());

                Button backspaceButton = activity.findViewById(R.id.pinBackspaceButton);
                backspaceButton.performClick();

                assertEquals(
                        "Backspace should clear the last bullet from the password field",
                        "",
                        passwordView.getText().toString());
                assertEquals(
                        "Backspace should remove"
                                + " the last password character from the builder",
                        0,
                        activity.getPasswordBuilder().length());
            });
        }
    }

    @Test
    public void testCapsLockUppercasesLetterKey() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                // letter 'a' is on the 3rd row (index 2) and first child
                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
                LinearLayout letterRow = (LinearLayout) keyboardContainer.getChildAt(2);
                Button aKey = (Button) letterRow.getChildAt(0);

                assertEquals(
                        "letter 'a' should be on the keyboard's third row and first column",
                        "a",
                        aKey.getText().toString());

                Button capsButton = activity.findViewById(R.id.capsButton);
                capsButton.performClick();

                // button instances are rebuilt; fetch again
                LinearLayout letterRowAfter = (LinearLayout) keyboardContainer.getChildAt(2);
                Button aKeyAfter = (Button) letterRowAfter.getChildAt(0);

                assertEquals("letter key should become uppercase after caps",
                        "A",
                        aKeyAfter.getText().toString());
            });
        }
    }

    @Test
    public void testChangeLayoutButtonTextSwitches() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button changeLayoutButton = activity.findViewById(R.id.changeKeyboardLayoutButton);

                String initialText = changeLayoutButton.getText().toString();
                String expectedInitial = activity.getString(R.string.special_chars);
                assertEquals("Initial change layout button text should show special chars",
                        expectedInitial, initialText);

                changeLayoutButton.performClick();

                String afterText = changeLayoutButton.getText().toString();
                String expectedAfter = activity.getString(R.string.abc);
                assertEquals("After toggling, change layout button should show abc",
                        expectedAfter, afterText);
            });
        }
    }

    @Test
    public void testSymbolKeyboardShowsExpectedCharacters() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button changeLayoutButton = activity.findViewById(R.id.changeKeyboardLayoutButton);
                changeLayoutButton.performClick();

                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
                assertEquals("Symbol layout should render three rows",
                        3,
                        keyboardContainer.getChildCount());

                String[][] expectedRows = {
                        {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"},
                        {"-", "_", "+", "=", "{", "}", "[", "]", "|", "\\"},
                        {":", ";", "<", ">", "?", "/", "~", ".", ",", "'"}
                };

                for (int rowIndex = 0; rowIndex < expectedRows.length; rowIndex++) {
                    LinearLayout symbolRow = (LinearLayout) keyboardContainer.getChildAt(rowIndex);
                    assertEquals("Each symbol row should keep the standard ten-key width",
                            expectedRows[rowIndex].length,
                            symbolRow.getChildCount());

                    for (int keyIndex = 0; keyIndex < expectedRows[rowIndex].length; keyIndex++) {
                        Button key = (Button) symbolRow.getChildAt(keyIndex);
                        assertEquals(
                                "The symbol keyboard should include the expected key at row "
                                        + rowIndex + ", index " + keyIndex,
                                expectedRows[rowIndex][keyIndex],
                                key.getText().toString());
                    }
                }
            });
        }
    }

    @Test
    public void testCapsLockDoesNotUppercaseSymbols() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.capsButton).performClick();
                activity.findViewById(R.id.changeKeyboardLayoutButton).performClick();

                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
                LinearLayout symbolRow = (LinearLayout) keyboardContainer.getChildAt(0);
                Button atKey = (Button) symbolRow.getChildAt(1);
                atKey.performClick();

                assertTrue("Caps lock should remain enabled in symbol layout",
                        activity.isCapsLockEnabled());
                assertEquals("Symbol layout should not transform symbols",
                        "@", atKey.getText().toString());
                assertEquals("Pressed symbol should be appended without casing",
                        "@", activity.getPasswordBuilder().toString());
            });
        }
    }

    @Test
    public void testModeSpecificUiTextForEachMode() {
        Map<String, String> expectedModes = Map.of(
                AppPasswordActivity.Mode.AUTHENTICATION.getModeName(), "Enter access code",
                AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName(), "Create access code",
                AppPasswordActivity.Mode.CHANGE_PASSWORD.getModeName(), "Create new access code",
                AppPasswordActivity.Mode.KEY_RECOVERY.getModeName(), "Create access code"
        );

        for (Map.Entry<String, String> modeExpectation : expectedModes.entrySet()) {
            String modeName = modeExpectation.getKey();
            String expectedText = modeExpectation.getValue();

            try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(
                    new Intent(context, AppPasswordActivity.class)
                            .putExtra(AppPasswordActivity.EXTRA_MODE, modeName))) {
                scenario.onActivity(activity -> {
                    TextView accessCodePrompt = activity.findViewById(R.id.accessCodePrompt);
                    assertEquals(
                            "Mode-specific label text should match the expected resource",
                            expectedText,
                            accessCodePrompt.getText().toString());
                });
            }
        }
    }

    @Test
    public void testActionBarBackNavigationIsEnabledOnlyForChangePasswordMode() {
        Map<String, Boolean> expectedModes = Map.of(
                AppPasswordActivity.Mode.AUTHENTICATION.getModeName(), false,
                AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName(), false,
                AppPasswordActivity.Mode.CHANGE_PASSWORD.getModeName(), true,
                AppPasswordActivity.Mode.KEY_RECOVERY.getModeName(), false
        );

        for (Map.Entry<String, Boolean> modeExpectation : expectedModes.entrySet()) {
            String modeName = modeExpectation.getKey();
            boolean shouldShowBackNavigation = modeExpectation.getValue();

            try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(
                    new Intent(context, AppPasswordActivity.class)
                            .putExtra(AppPasswordActivity.EXTRA_MODE, modeName))) {
                scenario.onActivity(activity -> {
                    ActionBar actionBar = activity.getSupportActionBar();
                    assertNotNull("Action bar should be available", actionBar);

                    boolean backNavigationEnabled = (actionBar.getDisplayOptions()
                            & ActionBar.DISPLAY_HOME_AS_UP) == ActionBar.DISPLAY_HOME_AS_UP;

                    assertEquals("Action bar title should remain empty",
                            "", actionBar.getTitle());
                    assertEquals(
                            "Back navigation visibility should match the password mode",
                            shouldShowBackNavigation, backNavigationEnabled);
                });
            }
        }
    }

    @Test
    public void testAuthenticationModeDisablesBackNavigation() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertTrue("Authentication mode should install an enabled back callback",
                        activity.getOnBackPressedDispatcher().hasEnabledCallbacks());

                activity.getOnBackPressedDispatcher().onBackPressed();

                assertTrue("Back press in authentication mode should finish the activity",
                        activity.isFinishing());
            });
        }
    }

    @Test
    public void testPasswordEntryUsesActualCharactersAndKeepsCapsAcrossLayoutToggle() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button capsButton = activity.findViewById(R.id.capsButton);
                Button changeLayoutButton = activity.findViewById(R.id.changeKeyboardLayoutButton);

                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);
                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);

                capsButton.performClick();
                assertTrue("Caps should be enabled after the toggle",
                        activity.isCapsLockEnabled());

                Button aKey = (Button) ((LinearLayout) keyboardContainer.getChildAt(2))
                        .getChildAt(0);
                aKey.performClick();
                assertEquals("A", activity.getPasswordBuilder().toString());
                assertEquals("•", passwordView.getText().toString());

                changeLayoutButton.performClick();
                assertTrue("Symbol layout should become active after toggling",
                        activity.isShowingSymbols());

                changeLayoutButton.performClick();
                assertFalse("Alpha layout should be active after toggling back",
                        activity.isShowingSymbols());

                LinearLayout alphaRow = (LinearLayout) keyboardContainer.getChildAt(2);
                Button alphaAKey = (Button) alphaRow.getChildAt(0);
                assertEquals(
                        "Caps state should persist across keyboard layout toggles",
                        "A",
                        alphaAKey.getText().toString());
            });
        }
    }

    @Test
    public void testOkButtonValidationForAuthenticationMode() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button okButton = activity.findViewById(R.id.okButton);
                okButton.performClick();

                assertEquals("Authentication mode"
                                + " should reject empty passwords without changing the builder",
                        0,
                        activity.getPasswordBuilder().length());
                assertFalse("Authentication mode"
                                + " should not finish the activity for an empty password",
                        activity.isFinishing());
            });
        }
    }

    @Test
    public void testOkButtonValidationForCreatePasswordMode() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                String testPassword = "1234";
                String testCensoredPassword = "•".repeat(testPassword.length());

                TextView accessCodePrompt = activity.findViewById(R.id.accessCodePrompt);
                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);

                activity.getPasswordBuilder().append(testPassword);
                passwordView.setText(testCensoredPassword);

                Button okButton = activity.findViewById(R.id.okButton);
                okButton.performClick();

                assertEquals("Create password mode"
                                + " should require a second confirmation step after"
                                + " a valid first entry",
                        activity.getString(R.string.repeat_access_code),
                        accessCodePrompt.getText().toString());
                assertEquals("The password buffer"
                                + " should clear after the first create-password validation pass",
                        0,
                        activity.getPasswordBuilder().length());
                assertEquals("The censored display should clear after the validation pass",
                        "",
                        passwordView.getText().toString());
            });
        }
    }

    @Test
    public void testCreatePasswordRejectsTooShortInput() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                enterPasswordUsingKeyboard(activity,
                        "1".repeat(CryptoSecrets.PASSWORD_MIN_LENGTH - 1));

                Button okButton = activity.findViewById(R.id.okButton);
                okButton.performClick();

                TextView accessCodePrompt = activity.findViewById(R.id.accessCodePrompt);
                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);

                assertEquals("Too-short input should retain the create-password label",
                        activity.getString(R.string.create_access_code),
                        accessCodePrompt.getText().toString());
                assertEquals("Too-short input should clear the password builder",
                        0, activity.getPasswordBuilder().length());
                assertEquals("Too-short input should clear the censored display",
                        "", passwordView.getText().toString());
            });
        }
    }

    @Test
    public void testCreatePasswordMismatchClearsInputAndKeepsConfirmationStep() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                enterPasswordUsingKeyboard(activity, "1234");
                activity.findViewById(R.id.okButton).performClick();

                TextView accessCodePrompt = activity.findViewById(R.id.accessCodePrompt);
                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);

                assertEquals("Valid first entry should open the confirmation step",
                        activity.getString(R.string.repeat_access_code),
                        accessCodePrompt.getText().toString());
                assertEquals("First entry should clear the password builder",
                        0, activity.getPasswordBuilder().length());
                assertEquals("First entry should clear the censored display",
                        "", passwordView.getText().toString());

                enterPasswordUsingKeyboard(activity, "1235");
                activity.findViewById(R.id.okButton).performClick();

                assertEquals("Mismatch should retain the confirmation step",
                        activity.getString(R.string.repeat_access_code),
                        accessCodePrompt.getText().toString());
                assertEquals("Mismatch should clear the password builder",
                        0, activity.getPasswordBuilder().length());
                assertEquals("Mismatch should clear the censored display",
                        "", passwordView.getText().toString());
            });
        }
    }

    @Test
    public void testChangeAndRecoveryModesRejectTooShortInput() {
        for (AppPasswordActivity.Mode mode : new AppPasswordActivity.Mode[]{
                AppPasswordActivity.Mode.CHANGE_PASSWORD,
                AppPasswordActivity.Mode.KEY_RECOVERY}) {
            Intent intent = new Intent(context, AppPasswordActivity.class)
                    .putExtra(AppPasswordActivity.EXTRA_MODE, mode.getModeName());

            try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    enterPasswordUsingKeyboard(activity,
                            "1".repeat(CryptoSecrets.PASSWORD_MIN_LENGTH - 1));
                    activity.findViewById(R.id.okButton).performClick();

                    assertEquals("Too-short input should clear " + mode + " password builder",
                            0, activity.getPasswordBuilder().length());
                    assertEquals("Too-short input should clear " + mode + " censored display",
                            "",
                            ((TextView) activity.findViewById(R.id.censoredPasswordTextView))
                                    .getText().toString());
                });
            }
        }
    }

    @Test
    public void testChangeAndRecoveryModesClearMismatchConfirmation() {
        for (AppPasswordActivity.Mode mode : new AppPasswordActivity.Mode[]{
                AppPasswordActivity.Mode.CHANGE_PASSWORD,
                AppPasswordActivity.Mode.KEY_RECOVERY}) {
            Intent intent = new Intent(context, AppPasswordActivity.class)
                    .putExtra(AppPasswordActivity.EXTRA_MODE, mode.getModeName());

            try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity -> {
                    enterPasswordUsingKeyboard(activity, "1234");
                    activity.findViewById(R.id.okButton).performClick();

                    assertEquals("Mismatch setup should show repeat label for " + mode,
                            activity.getString(R.string.repeat_access_code),
                            ((TextView) activity.findViewById(R.id.accessCodePrompt))
                                    .getText().toString());

                    enterPasswordUsingKeyboard(activity, "5678");
                    activity.findViewById(R.id.okButton).performClick();

                    assertEquals("Mismatch should clear " + mode + " password builder",
                            0, activity.getPasswordBuilder().length());
                    assertEquals("Mismatch should clear " + mode + " censored display",
                            "",
                            ((TextView) activity.findViewById(R.id.censoredPasswordTextView))
                                    .getText().toString());
                });
            }
        }
    }

    @Test
    public void testCreatePasswordModeValidationDependsOnKeyExistence() {
        when(appSecurityService.isKeyExists()).thenReturn(false);

        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.CREATE_PASSWORD.getModeName());

        try (ActivityScenario<AppPasswordActivity> ignored = ActivityScenario.launch(intent)) {
            verify(appSecurityService).isKeyExists();
        }

        when(appSecurityService.isKeyExists()).thenReturn(true);
        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Activity should finish when password creation is not permitted",
                    Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void testAuthenticationFailureFinishesActivityGracefully() {
        doThrow(new RuntimeException("Authentication service failed"))
                .when(appSecurityService).authenticate(any(char[].class));

        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                enterPasswordUsingKeyboard(activity, "1");
                activity.findViewById(R.id.okButton).performClick();

                assertTrue("Activity should finish when authentication fails",
                        activity.isFinishing());
            });
        }
    }

    @Test
    public void testOkButtonDoesNotCrashForEachMode() {
        for (AppPasswordActivity.Mode mode : AppPasswordActivity.Mode.values()) {
            Intent intent = new Intent(context, AppPasswordActivity.class)
                    .putExtra(AppPasswordActivity.EXTRA_MODE, mode.getModeName());

            try (ActivityScenario<AppPasswordActivity> s = ActivityScenario.launch(intent)) {
                s.onActivity(activity -> {
                    Button okButton = activity.findViewById(R.id.okButton);
                    TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);

                    okButton.performClick();

                    assertFalse("The activity "
                                    + "should not finish from an empty-password click",
                            activity.isFinishing());
                    assertEquals("The password builder"
                                    + " should remain empty after empty validation",
                            0,
                            activity.getPasswordBuilder().length());
                    assertEquals("The password display"
                                    + " should remain empty after empty validation",
                            "",
                            passwordView.getText().toString());
                });
            }
        }
    }

    @Test
    public void testBackspaceNoOpWhenEmpty() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);
                Button backspaceButton = activity.findViewById(R.id.pinBackspaceButton);

                // ensure empty
                assertEquals("password builder should initially be empty",
                        0, activity.getPasswordBuilder().length());
                assertEquals("password view should initially be empty",
                        "", passwordView.getText().toString());

                // pressing backspace when empty should be a no-op and not throw
                backspaceButton.performClick();

                assertEquals("password builder should remain empty after backspace",
                        0, activity.getPasswordBuilder().length());
                assertEquals("password view should remain empty after backspace",
                        "", passwordView.getText().toString());
            });
        }
    }

    @Test
    public void testBackspaceRemovesLastCharacterAcrossCapsAndSymbolInput() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button capsButton = activity.findViewById(R.id.capsButton);
                Button changeLayoutButton = activity.findViewById(R.id.changeKeyboardLayoutButton);
                Button backspaceButton = activity.findViewById(R.id.pinBackspaceButton);

                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);
                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);

                capsButton.performClick();

                Button letterKey = (Button) ((LinearLayout) keyboardContainer.getChildAt(2))
                        .getChildAt(0);
                letterKey.performClick();

                Button digitKey = (Button) ((LinearLayout) keyboardContainer.getChildAt(0))
                        .getChildAt(0);
                digitKey.performClick();

                changeLayoutButton.performClick();
                Button symbolKey = (Button) ((LinearLayout) keyboardContainer.getChildAt(0))
                        .getChildAt(0);
                symbolKey.performClick();

                assertEquals("Mixed input should keep the expected password builder state",
                        "A1!",
                        activity.getPasswordBuilder().toString());
                assertEquals("The password view"
                                + " should show bullet count for all appended characters",
                        "•••",
                        passwordView.getText().toString());

                backspaceButton.performClick();
                assertEquals("Backspace should remove the newest symbol after mixed input",
                        "A1",
                        activity.getPasswordBuilder().toString());
                assertEquals("Backspace should shorten the displayed bullet count by one",
                        "••",
                        passwordView.getText().toString());

                backspaceButton.performClick();
                assertEquals("Backspace should keep stripping characters in reverse order",
                        "A",
                        activity.getPasswordBuilder().toString());
                assertEquals("The display"
                                + " should be reduced by the second backspace as well",
                        "•",
                        passwordView.getText().toString());
            });
        }
    }

    @Test
    public void testAuthenticationModeBackNavigationFinishesAffinity() {
        Intent authIntent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(authIntent)) {
            scenario.onActivity(activity -> {
                assertTrue("Authentication mode should install a back callback",
                        activity.getOnBackPressedDispatcher().hasEnabledCallbacks());

                activity.getOnBackPressedDispatcher().onBackPressed();

                assertTrue("Authentication mode should finish the activity"
                                + " when back is pressed",
                        activity.isFinishing());
            });
        }
    }

    @Test
    public void testNonAuthenticationModesAllowStandardBackNavigation() {
        for (AppPasswordActivity.Mode mode : new AppPasswordActivity.Mode[]{
                AppPasswordActivity.Mode.CREATE_PASSWORD,
                AppPasswordActivity.Mode.CHANGE_PASSWORD,
                AppPasswordActivity.Mode.KEY_RECOVERY}) {
            Intent intent = new Intent(context, AppPasswordActivity.class)
                    .putExtra(AppPasswordActivity.EXTRA_MODE, mode.getModeName());

            try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
                scenario.onActivity(activity ->
                        assertFalse("Non-authentication modes should keep"
                                        + " the default back behavior",
                                activity.getOnBackPressedDispatcher().hasEnabledCallbacks()));
            }
        }
    }

    @Test
    public void testAddKeyboardRowSetsAllCapsWhenAppropriate() {
        Intent intent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE, AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AppPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                Button capsButton = activity.findViewById(R.id.capsButton);
                capsButton.performClick();

                LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
                LinearLayout letterRow = (LinearLayout) keyboardContainer.getChildAt(2);

                Button aKey = (Button) letterRow.getChildAt(0);

                assertEquals("When caps enabled, letter key text should be uppercase",
                        "A",
                        aKey.getText().toString());
            });
        }
    }
}
