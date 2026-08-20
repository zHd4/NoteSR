/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertEquals;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import app.notesr.R;

@RunWith(AndroidJUnit4.class)
public class AuthActivityTest {

    private Context context;
    private ActivityScenario<AuthActivity> scenario;

    @Before
    public void setUp() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.getUiAutomation().adoptShellPermissionIdentity();

        context = instrumentation.getTargetContext();

        Intent intent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHENTICATION.getModeName());

        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
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
    public void testKeyboardStartsWithAlphaNumericLayout() {
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

    @Test
    public void testCapsToggleAndBackspaceRemoveCharacters() {
        scenario.onActivity(activity -> {
            TextView passwordView = activity.findViewById(R.id.censoredPasswordTextView);
            LinearLayout keyboardContainer = activity.findViewById(R.id.keyboardContainer);
            Button firstKey = (Button) ((LinearLayout) keyboardContainer.getChildAt(0))
                    .getChildAt(0);

            firstKey.performClick();

            assertEquals(
                    "The password field should show one bullet after a key press",
                    "•",
                    passwordView.getText().toString());
            assertEquals(
                    "The password builder should contain one character after a key press",
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
                    "Backspace should remove the last password character from the builder",
                    0,
                    activity.getPasswordBuilder().length());
        });
    }

    @Test
    public void testCapsLockUppercasesLetterKey() {
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

    @Test
    public void testChangeLayoutButtonTextSwitches() {
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

    @Test
    public void testAuthButtonDoesNotCrashForEachMode() {
        for (AuthActivity.Mode mode : AuthActivity.Mode.values()) {
            Intent intent = new Intent(context, AuthActivity.class)
                    .putExtra(AuthActivity.EXTRA_MODE, mode.getModeName());

            try (ActivityScenario<AuthActivity> s = ActivityScenario.launch(intent)) {
                s.onActivity(activity -> {
                    Button authButton = activity.findViewById(R.id.authButton);
                    // Clicking should not throw,
                    // behavior is delegated to authHandler which is outside this test's scope
                    authButton.performClick();
                });
            }
        }
    }

    @Test
    public void testBackspaceNoOpWhenEmpty() {
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

    @Test
    public void testAddKeyboardRowSetsAllCapsWhenAppropriate() {
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
