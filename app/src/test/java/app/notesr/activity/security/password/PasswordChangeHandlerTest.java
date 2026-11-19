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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.widget.Toast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import app.notesr.R;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.service.security.rotation.SecretsRotationFailedException;
import app.notesr.service.security.rotation.SecretsRotationService;
import app.notesr.util.ActivityUtils;

class PasswordChangeHandlerTest {

    private AppPasswordActivity activity;
    private ActivityUtils activityUtils;
    private SecretsRotationService secretsRotationService;
    private PasswordFlowCompletionHelper completionHelper;
    private PasswordHelper passwordHelper;
    private PasswordChangeHandler handler;

    @BeforeEach
    void setUp() {
        activity = mock(AppPasswordActivity.class);
        activityUtils = mock(ActivityUtils.class);
        secretsRotationService = mock(SecretsRotationService.class);
        completionHelper = mock(PasswordFlowCompletionHelper.class);
        passwordHelper = mock(PasswordHelper.class);
        handler = new PasswordChangeHandler(activity, activityUtils, secretsRotationService,
                completionHelper, passwordHelper);
    }

    @Test
    void testChangePasswordWaitsWhenPasswordIsNotConfirmed() {
        when(passwordHelper.proceedPasswordSetting()).thenReturn(null);

        handler.changePassword();

        verifyNoInteractions(secretsRotationService);
        verifyNoInteractions(activityUtils);
        verifyNoInteractions(completionHelper);
    }

    @Test
    void testChangePasswordUpdatesSecretsShowsToastAndNavigates() {
        char[] password = "new-password".toCharArray();
        Intent notesListIntent = mock(Intent.class);

        when(passwordHelper.proceedPasswordSetting()).thenReturn(password);
        when(activity.getString(R.string.updated)).thenReturn("Updated");
        when(completionHelper.getNewActivityIntent(NotesListActivity.class))
                .thenReturn(notesListIntent);

        handler.changePassword();

        ArgumentCaptor<char[]> passwordCaptor = ArgumentCaptor.forClass(char[].class);
        verify(secretsRotationService).updatePassword(passwordCaptor.capture());
        assertArrayEquals(password, passwordCaptor.getValue(),
                "Secrets rotation should receive the confirmed password");

        verify(activityUtils).showToastMessage("Updated", Toast.LENGTH_SHORT);
        verify(completionHelper).getNewActivityIntent(NotesListActivity.class);
        verify(completionHelper).navigateNextActivity(notesListIntent);
    }

    @Test
    void testChangePasswordPropagatesInvalidPasswordWithoutCompletingFlow() {
        char[] password = "bad".toCharArray();

        IllegalArgumentException exception =
                new IllegalArgumentException("Invalid password");

        when(passwordHelper.proceedPasswordSetting()).thenReturn(password);
        doThrow(exception).when(secretsRotationService).updatePassword(password);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                handler::changePassword,
                "Invalid passwords should propagate their validation exception");

        assertEquals(exception, thrown,
                "The original invalid-password exception should be propagated");

        verify(activityUtils, never()).showToastMessage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(completionHelper, never()).getNewActivityIntent(NotesListActivity.class);
        verify(completionHelper, never()).navigateNextActivity(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void testChangePasswordPropagatesRotationFailureWithoutCompletingFlow() {
        char[] password = "new-password".toCharArray();

        SecretsRotationFailedException exception =
                new SecretsRotationFailedException("Rotation failed");

        when(passwordHelper.proceedPasswordSetting()).thenReturn(password);
        doThrow(exception).when(secretsRotationService).updatePassword(password);

        SecretsRotationFailedException thrown =
                assertThrows(SecretsRotationFailedException.class, handler::changePassword,
                        "Rotation failures should propagate to the caller");

        assertEquals(exception, thrown,
                "The original secrets-rotation exception should be propagated");

        verify(activityUtils, never()).showToastMessage(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt());
        verify(completionHelper, never()).getNewActivityIntent(NotesListActivity.class);
        verify(completionHelper, never()).navigateNextActivity(
                org.mockito.ArgumentMatchers.any());
    }
}
