/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.TextView;
import android.widget.Toast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.notesr.R;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.util.ActivityUtils;

class PasswordHelperTest {

    private AppPasswordActivity activity;
    private ActivityUtils activityUtils;
    private TextView topLabel;
    private TextView censoredPasswordView;
    private SecureStringBuilder passwordBuilder;
    private PasswordHelper passwordHelper;

    @BeforeEach
    void setUp() {
        activity = mock(AppPasswordActivity.class);
        activityUtils = mock(ActivityUtils.class);
        topLabel = mock(TextView.class);
        censoredPasswordView = mock(TextView.class);
        passwordBuilder = new SecureStringBuilder();

        when(activity.findViewById(R.id.authTopLabel)).thenReturn(topLabel);
        when(activity.findViewById(R.id.censoredPasswordTextView))
                .thenReturn(censoredPasswordView);

        passwordHelper = new PasswordHelper(activity, passwordBuilder, activityUtils);
    }

    @Test
    void testTooShortFirstEntryShowsMinimumLengthToastAndClearsInput() {
        passwordBuilder.append("1".repeat(CryptoSecrets.PASSWORD_MIN_LENGTH - 1));
        when(activity.getString(R.string.minimum_password_length_is_n))
                .thenReturn("Minimum password length is %d");

        char[] result = passwordHelper.proceedPasswordSetting();

        assertNull(result,
                "Too-short input should not complete password confirmation");
        assertEquals(0, passwordBuilder.length(),
                "Too-short input should clear the password builder");

        verify(censoredPasswordView).setText("");
        verify(activityUtils).showToastMessage(
                "Minimum password length is " + CryptoSecrets.PASSWORD_MIN_LENGTH,
                Toast.LENGTH_SHORT);
    }

    @Test
    void testExactMinimumLengthStartsConfirmationAndClearsInput() {
        passwordBuilder.append("1234");
        when(activity.getString(R.string.repeat_access_code))
                .thenReturn("Repeat access code");

        char[] result = passwordHelper.proceedPasswordSetting();

        assertNull(result,
                "Valid first entry should await confirmation");
        assertEquals(0, passwordBuilder.length(),
                "Valid first entry should clear the password builder");

        verify(topLabel).setText("Repeat access code");
        verify(censoredPasswordView).setText("");
    }

    @Test
    void testMatchingConfirmationReturnsPasswordAndClearsInput() {
        passwordBuilder.append("1234");
        passwordHelper.proceedPasswordSetting();

        passwordBuilder.append("1234");
        char[] result = passwordHelper.proceedPasswordSetting();

        assertArrayEquals("1234".toCharArray(), result,
                "Matching confirmation should return the entered password");
        assertEquals(0, passwordBuilder.length(),
                "Matching confirmation should clear the password builder");

        verify(censoredPasswordView, times(2)).setText("");
    }

    @Test
    void testMismatchingConfirmationShowsToastAndKeepsConfirmationStep() {
        passwordBuilder.append("1234");
        passwordHelper.proceedPasswordSetting();

        passwordBuilder.append("1235");
        when(activity.getString(R.string.code_not_match))
                .thenReturn("Codes do not match");

        char[] result = passwordHelper.proceedPasswordSetting();

        assertNull(result,
                "Mismatching confirmation should not complete password setup");
        assertEquals(0, passwordBuilder.length(),
                "Mismatching confirmation should clear the password builder");
        verify(activityUtils).showToastMessage(
                "Codes do not match", Toast.LENGTH_SHORT);

        verify(censoredPasswordView, times(2)).setText("");
    }

    @Test
    void testMismatchCanBeRetriedWithOriginalPassword() {
        passwordBuilder.append("1234");
        passwordHelper.proceedPasswordSetting();

        passwordBuilder.append("1235");
        passwordHelper.proceedPasswordSetting();

        passwordBuilder.append("1234");
        char[] result = passwordHelper.proceedPasswordSetting();

        assertArrayEquals("1234".toCharArray(), result,
                "A successful retry should return the original password");
        assertEquals(0, passwordBuilder.length(),
                "A successful retry should clear the password builder");
    }

    @Test
    void testResetPasswordClearsVisibleTextAndWipesBuilder() {
        passwordBuilder.append("secret");

        passwordHelper.resetPassword();

        assertEquals(0, passwordBuilder.length(),
                "Reset should clear the password builder length");
        assertArrayEquals(new char[0], passwordBuilder.toCharArray(),
                "Reset should wipe all buffered password characters");

        verify(censoredPasswordView).setText("");
    }
}
