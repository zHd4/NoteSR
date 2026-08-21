/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import app.notesr.R;
import io.bloco.faker.Faker;

@RunWith(AndroidJUnit4.class)
public class AuthActivityTest {

    private static final Faker FAKER = new Faker();

    private Context context;

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentation.getTargetContext();
    }

    @Test
    public void testModeFromStringMatchesKnownModes() {
        assertEquals("Authentication mode should map to the authentication constant",
                AuthActivity.Mode.AUTHENTICATION,
                AuthActivity.Mode.fromString(AuthActivity.Mode.AUTHENTICATION.getModeName()));
        assertEquals("Create password mode should map to the create password constant",
                AuthActivity.Mode.CREATE_PASSWORD,
                AuthActivity.Mode.fromString(AuthActivity.Mode.CREATE_PASSWORD.getModeName()));
        assertEquals("Change password mode should map to the change password constant",
                AuthActivity.Mode.CHANGE_PASSWORD,
                AuthActivity.Mode.fromString(AuthActivity.Mode.CHANGE_PASSWORD.getModeName()));
        assertEquals("Key recovery mode should map to the key recovery constant",
                AuthActivity.Mode.KEY_RECOVERY,
                AuthActivity.Mode.fromString(AuthActivity.Mode.KEY_RECOVERY.getModeName()));
    }

    @Test
    public void testModeFromStringThrowsForUnknownMode() {
        IllegalArgumentException exception = assertThrows(
                "Unknown modes should throw an exception",
                IllegalArgumentException.class,
                () -> AuthActivity.Mode.fromString("invalid"));

        assertNotNull("Exception message should not be null", exception.getMessage());
        assertTrue("The exception message should include the invalid value",
                exception.getMessage().contains("invalid"));
    }

    @Test
    public void testModeFromStringThrowsForNullMode() {
        IllegalArgumentException exception = assertThrows(
                "Missing or null modes should be rejected consistently",
                IllegalArgumentException.class,
                () -> AuthActivity.Mode.fromString(null));

        assertNotNull("Null-mode exception message should not be null",
                exception.getMessage());
        assertTrue("The null-mode exception message should mention the null value",
                exception.getMessage().contains("null"));
    }

    @Test
    public void testKeyboardStartsWithAlphaNumericLayout() {
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
    public void testCapsToggleAndBackspaceRemoveCharacters() {
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
    public void testModeSpecificUiTextForEachMode() {
        Map<String, String> expectedModes = Map.of(
                AuthActivity.Mode.AUTHENTICATION.getModeName(), "Enter access code",
                AuthActivity.Mode.CREATE_PASSWORD.getModeName(), "Create access code",
                AuthActivity.Mode.CHANGE_PASSWORD.getModeName(), "Create new access code",
                AuthActivity.Mode.KEY_RECOVERY.getModeName(), "Create access code"
        );

        for (Map.Entry<String, String> modeExpectation : expectedModes.entrySet()) {
            String modeName = modeExpectation.getKey();
            String expectedText = modeExpectation.getValue();

            try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(
                    new Intent(context, AuthActivity.class)
                            .putExtra(AuthActivity.EXTRA_MODE, modeName))) {
                scenario.onActivity(activity -> {
                    TextView topLabel = activity.findViewById(R.id.authTopLabel);
                    assertEquals(
                            "Mode-specific label text should match the expected resource",
                            expectedText,
                            topLabel.getText().toString());
                });
            }
        }
    }

    @Test
    public void testPasswordEntryUsesActualCharactersAndKeepsCapsAcrossLayoutToggle() {
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.CREATE_PASSWORD.getModeName());

        try (ActivityScenario<AuthActivity> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                String testPassword = FAKER.internet.password();
                String testCensoredPassword = "•".repeat(testPassword.length());

                TextView topLabel = activity.findViewById(R.id.authTopLabel);
                TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);

                activity.getPasswordBuilder().append(testPassword);
                passwordView.setText(testCensoredPassword);

                Button okButton = activity.findViewById(R.id.okButton);
                okButton.performClick();

                assertEquals("Create password mode"
                                + " should require a second confirmation step after"
                                + " a valid first entry",
                        activity.getString(R.string.repeat_access_code),
                        topLabel.getText().toString());
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
    public void testOkButtonDoesNotCrashForEachMode() {
        for (AuthActivity.Mode mode : AuthActivity.Mode.values()) {
            Intent intent = new Intent(context, AuthActivity.class)
                    .putExtra(AuthActivity.EXTRA_MODE, mode.getModeName());

            try (ActivityScenario<AuthActivity> s = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
    public void testAuthenticationModeDisablesBackNavigation() {
        Intent authIntent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(authIntent)) {
            scenario.onActivity(activity ->
                    assertTrue("Authentication mode should install a back callback",
                    activity.getOnBackPressedDispatcher().hasEnabledCallbacks()));
        }

        Intent createIntent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.CREATE_PASSWORD.getModeName());

        try (ActivityScenario<AuthActivity> createScenario =
                     ActivityScenario.launch(createIntent)) {
            createScenario.onActivity(activity ->
                    assertFalse("Create-password mode should not disable back navigation",
                    activity.getOnBackPressedDispatcher().hasEnabledCallbacks()));
        }
    }

    @Test
    public void testAddKeyboardRowSetsAllCapsWhenAppropriate() {
        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        try (ActivityScenario<AuthActivity> scenario = ActivityScenario.launch(intent)) {
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
