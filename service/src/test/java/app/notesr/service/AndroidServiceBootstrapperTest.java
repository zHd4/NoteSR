/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import app.notesr.core.security.dto.CryptoSecrets;

class AndroidServiceBootstrapperTest {

    private AndroidServiceRegistry registry;
    private AndroidServiceBootstrapper bootstrapper;
    private Context context;
    private CryptoSecrets secrets;

    public static class TestService extends AndroidService {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @NonNull
        @Override
        protected AndroidServiceEntry getEntry(String payload, String state) {
            return null;
        }
    }

    public static class MockStarter implements AndroidServiceStarter {
        public static AndroidServiceStarter mockInstance = mock(AndroidServiceStarter.class);

        public MockStarter() {
        }

        @Override
        public void start(Context context) throws Exception {
            mockInstance.start(context);
        }

        @Override
        public void start(Context context, CryptoSecrets secrets, String payload, String state)
                throws Exception {
            mockInstance.start(context, secrets, payload, state);
        }
    }

    public static class FailingStarter implements AndroidServiceStarter {
        public FailingStarter() {
        }

        @Override
        public void start(Context context) throws Exception {
            throw new Exception("Start failed");
        }

        @Override
        public void start(Context context, CryptoSecrets secrets, String payload, String state)
                throws Exception {
            throw new Exception("Start failed");
        }
    }

    public static class NoDefaultConstructorStarter implements AndroidServiceStarter {
        public NoDefaultConstructorStarter(String arg) {
        }

        @Override
        public void start(Context context) {
        }

        @Override
        public void start(Context context, CryptoSecrets secrets, String payload, String state) {
        }
    }

    @BeforeEach
    void setUp() {
        registry = mock(AndroidServiceRegistry.class);
        bootstrapper = new AndroidServiceBootstrapper(registry);
        context = mock(Context.class);
        secrets = mock(CryptoSecrets.class);
        MockStarter.mockInstance = mock(AndroidServiceStarter.class);
    }

    @Test
    void testStartServicesPreAuthStartsOnlyNonAuthAutoStartServices() throws Exception {
        AndroidServiceEntry authService = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(MockStarter.class)
                .autoStart(true)
                .requiresAuth(true)
                .build();

        AndroidServiceEntry preAuthService = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(MockStarter.class)
                .autoStart(true)
                .requiresAuth(false)
                .build();

        AndroidServiceEntry noAutoStartService = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(MockStarter.class)
                .autoStart(false)
                .requiresAuth(false)
                .build();

        Set<AndroidServiceEntry> entries = new HashSet<>();
        entries.add(authService);
        entries.add(preAuthService);
        entries.add(noAutoStartService);

        when(registry.getSet()).thenReturn(entries);

        bootstrapper.startServicesPreAuth(context);

        verify(MockStarter.mockInstance, times(1)).start(context);
        verify(MockStarter.mockInstance, never()).start(any(), any(), any(), any());
    }

    @Test
    void testStartServicesPostAuthStartsOnlyAuthAutoStartServices() throws Exception {
        AndroidServiceEntry authService = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(MockStarter.class)
                .autoStart(true)
                .requiresAuth(true)
                .build();

        AndroidServiceEntry preAuthService = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(MockStarter.class)
                .autoStart(true)
                .requiresAuth(false)
                .build();

        Set<AndroidServiceEntry> entries = new HashSet<>();
        entries.add(authService);
        entries.add(preAuthService);

        when(registry.getSet()).thenReturn(entries);

        bootstrapper.startServicesPostAuth(context, secrets);

        verify(MockStarter.mockInstance, times(1))
                .start(context, secrets, null, null);
        verify(MockStarter.mockInstance, never()).start(context);
    }

    @Test
    void testStartServicesPostAuthThrowsNpeWhenSecretsNull() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> bootstrapper.startServicesPostAuth(context, null),
                "Should throw NPE when secrets are null");
        assertEquals("Secrets are null", exception.getMessage(),
                "Exception message should match");
    }

    @Test
    void testStartServiceWithPayloadAndState() throws Exception {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(MockStarter.class)
                .autoStart(true)
                .requiresAuth(false)
                .payload("testPayload")
                .state("testState")
                .build();

        when(registry.getSet()).thenReturn(Collections.singleton(entry));

        bootstrapper.startServicesPreAuth(context);

        verify(MockStarter.mockInstance, times(1))
                .start(context, null, "testPayload", "testState");
        verify(MockStarter.mockInstance, never()).start(context);
    }

    @Test
    void testStartServiceThrowsRuntimeWhenStarterFails() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(FailingStarter.class)
                .autoStart(true)
                .requiresAuth(false)
                .build();

        when(registry.getSet()).thenReturn(Collections.singleton(entry));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bootstrapper.startServicesPreAuth(context),
                "Should throw RuntimeException when starter fails");
        assertEquals("Failed to start service " + TestService.class.getCanonicalName(),
                exception.getMessage(), "Exception message should match");
    }

    @Test
    void testStartServiceThrowsRuntimeWhenNoDefaultConstructor() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService.class)
                .starterClass(NoDefaultConstructorStarter.class)
                .autoStart(true)
                .requiresAuth(false)
                .build();

        when(registry.getSet()).thenReturn(Collections.singleton(entry));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bootstrapper.startServicesPreAuth(context),
                "Should throw RuntimeException when starter has no default constructor");
        assertEquals("Starter class of service " + TestService.class.getCanonicalName()
                        + " does not have a default constructor", exception.getMessage(),
                "Exception message should match");
    }
}
