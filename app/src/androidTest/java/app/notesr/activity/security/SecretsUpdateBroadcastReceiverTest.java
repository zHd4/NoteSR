/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import app.notesr.service.security.rotation.SecretsUpdateAndroidService;

public class SecretsUpdateBroadcastReceiverTest {

    @Test
    public void testOnReceiveWithCompletedSecretsUpdateInvokesCompleteCallback() {
        AtomicBoolean completeCallbackCalled = new AtomicBoolean(false);
        AtomicBoolean failureCallbackCalled = new AtomicBoolean(false);

        Runnable onComplete = () -> completeCallbackCalled.set(true);
        Runnable onFailure = () -> failureCallbackCalled.set(true);

        SecretsUpdateBroadcastReceiver receiver = new SecretsUpdateBroadcastReceiver(onComplete,
                onFailure);

        Intent intent = new Intent(SecretsUpdateAndroidService.BROADCAST_ACTION);
        intent.putExtra(SecretsUpdateAndroidService.EXTRA_COMPLETE, true);

        receiver.onReceive(null, intent);
        assertTrue("Complete callback should be called when secrets update is complete",
                completeCallbackCalled.get());
        assertFalse("Failure callback should not be called when update is complete",
                failureCallbackCalled.get());
    }

    @Test
    public void testOnReceiveWithFailedSecretsUpdateInvokesFailureCallback() {
        AtomicBoolean completeCallbackCalled = new AtomicBoolean(false);
        AtomicBoolean failureCallbackCalled = new AtomicBoolean(false);

        Runnable onComplete = () -> completeCallbackCalled.set(true);
        Runnable onFailure = () -> failureCallbackCalled.set(true);

        SecretsUpdateBroadcastReceiver receiver = new SecretsUpdateBroadcastReceiver(onComplete,
                onFailure);

        Intent intent = new Intent(SecretsUpdateAndroidService.BROADCAST_ACTION);
        intent.putExtra(SecretsUpdateAndroidService.EXTRA_FAIL, true);

        receiver.onReceive(null, intent);
        assertFalse("Complete callback should not be called when secrets update fails",
                completeCallbackCalled.get());
        assertTrue("Failure callback should be called when secrets update fails",
                failureCallbackCalled.get());
    }

    @Test
    public void testOnReceiveWithNoCompletionFlagsThrowsIllegalStateException() {
        AtomicBoolean completeCallbackCalled = new AtomicBoolean(false);
        AtomicBoolean failureCallbackCalled = new AtomicBoolean(false);

        Runnable onComplete = () -> completeCallbackCalled.set(true);
        Runnable onFailure = () -> failureCallbackCalled.set(true);

        SecretsUpdateBroadcastReceiver receiver = new SecretsUpdateBroadcastReceiver(onComplete,
                onFailure);

        Intent intent = new Intent(SecretsUpdateAndroidService.BROADCAST_ACTION);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> receiver.onReceive(null, intent));

        assertNotNull("Exception message should not be null", exception.getMessage());
        assertTrue("Exception should mention the unexpected intent",
                exception.getMessage().contains("Unexpected intent received"));
        assertFalse("Complete callback should not be called for unexpected intent",
                completeCallbackCalled.get());
        assertFalse("Failure callback should not be called for unexpected intent",
                failureCallbackCalled.get());
    }

    @Test
    public void testOnReceiveWithWrongActionDoesNotInvokeCallbacks() {
        AtomicBoolean completeCallbackCalled = new AtomicBoolean(false);
        AtomicBoolean failureCallbackCalled = new AtomicBoolean(false);

        Runnable onComplete = () -> completeCallbackCalled.set(true);
        Runnable onFailure = () -> failureCallbackCalled.set(true);

        SecretsUpdateBroadcastReceiver receiver = new SecretsUpdateBroadcastReceiver(onComplete,
                onFailure);

        Intent intent = new Intent("some.other.ACTION");
        intent.putExtra(SecretsUpdateAndroidService.EXTRA_COMPLETE, true);

        receiver.onReceive(null, intent);
        assertFalse("Complete callback should not be called for wrong action",
                completeCallbackCalled.get());
        assertFalse("Failure callback should not be called for wrong action",
                failureCallbackCalled.get());
    }
}
