/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import app.notesr.R;
import app.notesr.activity.security.KeyRecoveryActivity;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.AuthenticationFailedException;
import app.notesr.util.ActivityUtils;

class AuthenticationHandlerTest {

    private AppPasswordActivity activity;
    private AppSecurityService appSecurityService;
    private SecureStringBuilder passwordBuilder;
    private ActivityUtils activityUtils;
    private PasswordFlowCompletionHelper completionHelper;
    private PasswordHelper passwordHelper;
    private TestAuthenticationHandler handler;
    private MockedStatic<Log> logMock;

    @BeforeEach
    void setUp() {
        logMock = mockStatic(Log.class);
        activity = mock(AppPasswordActivity.class);
        appSecurityService = mock(AppSecurityService.class);
        passwordBuilder = new SecureStringBuilder();
        activityUtils = mock(ActivityUtils.class);
        completionHelper = mock(PasswordFlowCompletionHelper.class);
        passwordHelper = mock(PasswordHelper.class);

        when(activity.getString(R.string.enter_the_code)).thenReturn("Enter the code!");
        when(activity.getString(R.string.blocked)).thenReturn("Blocked!");
        when(activity.getString(R.string.wrong_code_you_have_n_attempts))
                .thenReturn("Wrong code, you have %d attempts remaining");

        handler = new TestAuthenticationHandler(activity, appSecurityService,
                passwordBuilder, activityUtils, completionHelper, passwordHelper);
    }

    @AfterEach
    void tearDown() {
        logMock.close();
    }

    @Test
    void testAuthenticateEmptyPasswordShowsPromptWithoutAuthenticating() {
        handler.authenticate();

        verify(activityUtils).showToastMessage("Enter the code!", Toast.LENGTH_SHORT);
        verifyNoInteractions(appSecurityService);
        verifyNoInteractions(completionHelper);
        verify(passwordHelper, never()).resetPassword();
    }

    @Test
    void testAuthenticateValidPasswordCompletesPasswordFlow() {
        passwordBuilder.append("1234");

        Context applicationContext = mock(Context.class);
        Intent notesIntent = mock(Intent.class);

        when(activity.getApplicationContext()).thenReturn(applicationContext);
        when(completionHelper.getNewActivityIntent(
                app.notesr.activity.note.list.NotesListActivity.class))
                .thenReturn(notesIntent);

        handler.authenticate();

        ArgumentCaptor<char[]> password = ArgumentCaptor.forClass(char[].class);
        verify(appSecurityService).authenticate(password.capture());

        assertArrayEquals("1234".toCharArray(), password.getValue(),
                "Authentication should receive the entered password");

        verify(passwordHelper).resetPassword();
        verify(completionHelper).bootstrapPostAuthServices(applicationContext);
        verify(completionHelper).navigateNextOrIntermediateActivity(notesIntent);
    }

    @Test
    void testAuthenticateFirstFailureShowsTwoAttemptsRemainingAndRetries() {
        passwordBuilder.append("wrong");
        doThrow(new AuthenticationFailedException("Invalid password"))
                .when(appSecurityService).authenticate(ArgumentMatchers.any(char[].class));

        handler.authenticate();

        assertEquals(1, handler.sleepCount,
                "A first failed attempt should wait before retrying");
        verify(activityUtils).showToastMessage(
                "Wrong code, you have 2 attempts remaining", Toast.LENGTH_SHORT);

        verify(passwordHelper).resetPassword();
        verify(appSecurityService, never()).blockApp();
    }

    @Test
    void testAuthenticateSecondFailureShowsOneAttemptRemainingAndRetries() {
        passwordBuilder.append("wrong");

        doThrow(new AuthenticationFailedException("Invalid password"))
                .when(appSecurityService).authenticate(ArgumentMatchers.any(char[].class));

        handler.authenticate();
        passwordBuilder.append("again");

        handler.authenticate();

        assertEquals(2, handler.sleepCount,
                "Each of the first two failed attempts should wait before retrying");
        verify(activityUtils).showToastMessage(
                "Wrong code, you have 1 attempts remaining", Toast.LENGTH_SHORT);

        verify(passwordHelper, times(2)).resetPassword();
        verify(appSecurityService, never()).blockApp();
    }

    @Test
    void testAuthenticateThirdFailureBlocksAndNavigatesToKeyRecovery() {
        passwordBuilder.append("wrong");

        doThrow(new AuthenticationFailedException("Invalid password"))
                .when(appSecurityService).authenticate(ArgumentMatchers.any(char[].class));

        Intent recoveryIntent = mock(Intent.class);
        when(completionHelper.getNewActivityIntent(KeyRecoveryActivity.class))
                .thenReturn(recoveryIntent);
        when(recoveryIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK))
                .thenReturn(recoveryIntent);

        handler.authenticate();
        passwordBuilder.append("wrong");
        handler.authenticate();
        passwordBuilder.append("wrong");
        handler.authenticate();

        assertEquals(2, handler.sleepCount,
                "Only the first two failed attempts should wait before retrying");

        verify(appSecurityService).blockApp();
        verify(activityUtils).showToastMessage("Blocked!", Toast.LENGTH_SHORT);
        verify(recoveryIntent).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        verify(completionHelper).navigateNextActivity(recoveryIntent);
        verify(passwordHelper, times(3)).resetPassword();
    }

    @Test
    void testAuthenticateUnexpectedSecurityErrorIsPropagated() {
        passwordBuilder.append("secret");

        AppSecurityException exception =
                new AppSecurityException("Unexpected security failure");

        doThrow(exception).when(appSecurityService).authenticate(
                org.mockito.ArgumentMatchers.any(char[].class));

        AppSecurityException thrown = assertThrows(AppSecurityException.class,
                handler::authenticate,
                "Unexpected security errors should be propagated");

        assertEquals(exception, thrown,
                "The original unexpected security exception should be thrown");

        verify(passwordHelper, never()).resetPassword();
        verify(appSecurityService, never()).blockApp();
    }

    private static final class TestAuthenticationHandler extends AuthenticationHandler {
        private int sleepCount;

        private TestAuthenticationHandler(AppPasswordActivity activity,
                                          AppSecurityService appSecurityService,
                                          SecureStringBuilder passwordBuilder,
                                          ActivityUtils activityUtils,
                                          PasswordFlowCompletionHelper completionHelper,
                                          PasswordHelper passwordHelper) {
            super(activity, appSecurityService, passwordBuilder, activityUtils,
                    completionHelper, passwordHelper);
        }

        @Override
        protected void sleepBeforeRetry() {
            sleepCount++;
        }
    }
}
