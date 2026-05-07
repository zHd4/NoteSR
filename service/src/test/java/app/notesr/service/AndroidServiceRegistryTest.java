/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.NonNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import app.notesr.core.security.dto.CryptoSecrets;

class AndroidServiceRegistryTest {

    private AndroidServiceRegistry registry;
    private SharedPreferences.Editor editor;

    private static class MockStarter1 implements AndroidServiceStarter {
        @Override public void start(Context context) {}
        @Override public void start(Context context, CryptoSecrets secrets, String payload, String state) {}
    }

    private static class MockStarter2 implements AndroidServiceStarter {
        @Override public void start(Context context) {}
        @Override public void start(Context context, CryptoSecrets secrets, String payload, String state) {}
    }

    private static class TestService1 extends AndroidService {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @NonNull
        @Override
        protected AndroidServiceEntry getEntry(String payload, String state) {
            return AndroidServiceEntry.builder()
                    .serviceClass(TestService1.class)
                    .starterClass(MockStarter1.class)
                    .serviceName("TestService1")
                    .payload(payload)
                    .state(state)
                    .build();
        }
    }

    private static class TestService2 extends AndroidService {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @NonNull
        @Override
        protected AndroidServiceEntry getEntry(String payload, String state) {
            return AndroidServiceEntry.builder()
                    .serviceClass(TestService2.class)
                    .starterClass(MockStarter1.class)
                    .serviceName("TestService2")
                    .payload(payload)
                    .state(state)
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);

        when(prefs.edit()).thenReturn(editor);
        when(editor.putStringSet(any(), any())).thenReturn(editor);

        registry = new AndroidServiceRegistry(prefs);
    }

    @Test
    void testRegisterAndIsServiceRunning() {
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running initially");

        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build());

        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running after registration");
        assertFalse(registry.isServiceRunning(TestService2.class),
                "Service 2 should not be running");

        verify(editor, times(2))
                .putStringSet(eq("current_running_services"), any());
        verify(editor, times(2)).apply();
    }

    @Test
    void testUnregister() {
        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build());
        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running after registration");

        registry.unregister(TestService1.class);
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running after unregistration");

        verify(editor, times(3)).apply();
    }

    @Test
    void testGetSetReturnsACopy() {
        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build());
        Set<AndroidServiceEntry> services = registry.getSet();

        assertEquals(1, services.size(),
                "The set should contain 1 service");
        assertTrue(services.stream().anyMatch(e ->
                e.getServiceClass().equals(TestService1.class)),
            "The set should contain TestService1");

        services.clear();
        assertFalse(registry.getSet().isEmpty(),
                "Modifying the returned set should not affect the registry");
    }

    @Test
    void testMultipleServices() {
        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build());
        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService2.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 2")
                .build());

        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running");
        assertTrue(registry.isServiceRunning(TestService2.class),
                "Service 2 should be running");
        assertEquals(2, registry.getSet().size(),
                "Registry should contain 2 services");

        registry.unregister(TestService1.class);

        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running after unregistration");
        assertTrue(registry.isServiceRunning(TestService2.class),
                "Service 2 should still be running");
        assertEquals(1, registry.getSet().size(),
                "Registry should contain 1 service after unregistration");
    }

    @Test
    void testRegisterWithAutoStart() {
        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .autoStart(true)
                .build());
        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running");

        AndroidServiceEntry entry = registry.getSet().iterator().next();
        assertTrue(entry.isAutoStart(),
                "Service should have autoStart set to true");
    }

    @Test
    void testLoadFromPrefs() throws Exception {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .autoStart(true)
                .build();

        Set<String> jsonSet = new HashSet<>();
        jsonSet.add(entry.toJson());

        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockPrefs.getStringSet(eq("current_running_services"), eq(null)))
                .thenReturn(jsonSet);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putStringSet(any(), any())).thenReturn(mockEditor);

        AndroidServiceRegistry newRegistry = new AndroidServiceRegistry(mockPrefs);
        assertTrue(newRegistry.isServiceRunning(TestService1.class),
                "Service 1 should be running after loading from prefs");

        AndroidServiceEntry loadedEntry = newRegistry.getSet().iterator().next();
        assertTrue(loadedEntry.isAutoStart(),
                "Loaded service should have autoStart set to true");
    }

    @Test
    void testLoadFromPrefsSkipsNonAutoStart() throws Exception {
        AndroidServiceEntry autoStartEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .autoStart(true)
                .build();
        AndroidServiceEntry noAutoStartEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService2.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 2")
                .autoStart(false)
                .build();

        Set<String> jsonSet = new HashSet<>();
        jsonSet.add(autoStartEntry.toJson());
        jsonSet.add(noAutoStartEntry.toJson());

        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockPrefs.getStringSet(eq("current_running_services"), eq(null)))
                .thenReturn(jsonSet);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putStringSet(any(), any())).thenReturn(mockEditor);

        AndroidServiceRegistry newRegistry = new AndroidServiceRegistry(mockPrefs);
        assertTrue(newRegistry.isServiceRunning(TestService1.class),
                "Auto-start service should be loaded");
        assertFalse(newRegistry.isServiceRunning(TestService2.class),
                "Non auto-start service should be skipped");
    }

    @Test
    void testUpdateEntry() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .payload("initial payload")
                .state("initial state")
                .build();

        registry.register(entry);

        AndroidServiceEntry updatedEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .payload("updated payload")
                .state("updated state")
                .build();

        registry.updateEntry(updatedEntry);

        AndroidServiceEntry result = registry.getSet().stream()
                .filter(e -> e.getServiceClass().equals(TestService1.class))
                .findFirst()
                .orElseThrow();

        assertEquals("updated payload", result.getPayload(),
                "Service payload should be updated");
        assertEquals("updated state", result.getState(),
                "Service state should be updated");
        verify(editor, times(3)).apply();
    }

    @Test
    void testRegisterExistingServiceUpdatesEntry() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .payload("initial payload")
                .state("initial state")
                .build();

        registry.register(entry);

        AndroidServiceEntry updatedEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .payload("updated payload")
                .state("updated state")
                .build();

        registry.register(updatedEntry);

        assertEquals(1, registry.getSet().size(),
                "Registry should still contain only 1 service");

        AndroidServiceEntry result = registry.getSet().iterator().next();
        assertEquals("updated payload", result.getPayload(),
                "Service payload should be updated via register");
        assertEquals("updated state", result.getState(),
                "Service state should be updated via register");
        verify(editor, times(3)).apply();
    }

    @Test
    void testRegisterExistingServiceWithMismatchedStarterClass() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build();
        registry.register(entry);

        AndroidServiceEntry mismatchedStarter = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter2.class)
                .serviceName("Test Service 1")
                .build();

        assertThrows(IllegalArgumentException.class, () -> registry.register(mismatchedStarter),
                "register should throw IllegalArgumentException when starter class mismatches");
    }

    @Test
    void testRegisterExistingServiceWithMismatchedServiceName() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build();
        registry.register(entry);

        AndroidServiceEntry mismatchedName = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Different Name")
                .build();

        assertThrows(IllegalArgumentException.class, () -> registry.register(mismatchedName),
                "register should throw IllegalArgumentException when service name mismatches");
    }

    @Test
    void testUpdateEntryDoesNotUpdateImmutableFields() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .autoStart(true)
                .requiresAuth(false)
                .build();
        registry.register(entry);

        AndroidServiceEntry updatedEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .autoStart(false) // This should NOT be updated
                .requiresAuth(true) // This should NOT be updated
                .payload("new payload")
                .state("new state")
                .build();

        registry.updateEntry(updatedEntry);

        AndroidServiceEntry result = registry.getSet().iterator().next();
        assertTrue(result.isAutoStart(), "autoStart should remain immutable");
        assertFalse(result.isRequiresAuth(), "requiresAuth should remain immutable");
        assertEquals("new payload", result.getPayload());
        assertEquals("new state", result.getState());
    }

    @Test
    void testUnregisterNonExistentService() {
        registry.unregister(TestService1.class);
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running");
        verify(editor, times(2)).apply();
    }

    @Test
    void testUpdateNonExistentEntry() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .state("new state")
                .build();

        registry.updateEntry(entry);
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running after updating non-existent entry");
        verify(editor, times(2)).apply();
    }

    @Test
    void testUnregisterNullClass() {
        assertThrows(NullPointerException.class, () -> registry.unregister(null),
                "Should throw NullPointerException when unregistering null class");
    }

    @Test
    void testIsServiceRunningNullClass() {
        assertThrows(NullPointerException.class, () -> registry.isServiceRunning(null),
                "Should throw NullPointerException when checking if null class is running");
    }

    @Test
    void testRegisterNullEntry() {
        assertThrows(NullPointerException.class, () -> registry.register(null),
                "Should throw NullPointerException when registering null entry");
    }

    @Test
    void testRegisterEntryWithNullFields() {
        AndroidServiceEntry entryMissingClass = AndroidServiceEntry.builder()
                .starterClass(MockStarter1.class)
                .serviceName("Test")
                .build();
        assertThrows(NullPointerException.class, () -> registry.register(entryMissingClass),
                "Should throw NullPointerException when service class is null");

        AndroidServiceEntry entryMissingStarter = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .serviceName("Test")
                .build();
        assertThrows(NullPointerException.class, () -> registry.register(entryMissingStarter),
                "Should throw NullPointerException when starter class is null");

        AndroidServiceEntry entryMissingName = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .build();
        assertThrows(NullPointerException.class, () -> registry.register(entryMissingName),
                "Should throw NullPointerException when service name is null");
    }

    @Test
    void testUpdateEntryWithNullFields() {
        AndroidServiceEntry entryMissingClass = AndroidServiceEntry.builder()
                .starterClass(MockStarter1.class)
                .serviceName("Test")
                .build();
        assertThrows(NullPointerException.class, () -> registry.updateEntry(entryMissingClass),
                "Should throw NullPointerException when updating entry with null service class");

        AndroidServiceEntry entryMissingStarter = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .serviceName("Test")
                .build();
        assertThrows(NullPointerException.class, () -> registry.updateEntry(entryMissingStarter),
                "Should throw NullPointerException when updating entry with null starter class");

        AndroidServiceEntry entryMissingName = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .build();
        assertThrows(NullPointerException.class, () -> registry.updateEntry(entryMissingName),
                "Should throw NullPointerException when updating entry with null service name");
    }

    @Test
    void testUpdateEntryWithMismatchedStarterClass() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build();
        registry.register(entry);

        AndroidServiceEntry mismatchedStarter = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter2.class) // Different starter class
                .serviceName("Test Service 1")
                .build();

        assertThrows(IllegalArgumentException.class, () -> registry.updateEntry(mismatchedStarter),
                "updateEntry should throw IllegalArgumentException when starter class mismatches");
    }

    @Test
    void testUpdateEntryWithMismatchedServiceName() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Test Service 1")
                .build();
        registry.register(entry);

        AndroidServiceEntry mismatchedName = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Different Name")
                .build();

        assertThrows(IllegalArgumentException.class, () -> registry.updateEntry(mismatchedName),
                "updateEntry should throw IllegalArgumentException when service name mismatches");
    }

    @Test
    void testUpdateNullEntry() {
        assertThrows(NullPointerException.class, () -> registry.updateEntry(null),
                "Should throw NullPointerException when updating null entry");
    }

    @Test
    void testDeserializationError() {
        Set<String> jsonSet = new HashSet<>();
        jsonSet.add("invalid json");

        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getStringSet(eq("current_running_services"), eq(null)))
                .thenReturn(jsonSet);

        assertThrows(RuntimeException.class, () -> new AndroidServiceRegistry(mockPrefs),
                "Should throw RuntimeException when deserialization fails");
    }

    @Test
    void testFullEntryPersistence() {
        AndroidServiceEntry entry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
                .starterClass(MockStarter1.class)
                .serviceName("Full Service")
                .autoStart(true)
                .requiresAuth(true)
                .payload("test payload")
                .state("test state")
                .build();

        registry.register(entry);

        Set<AndroidServiceEntry> services = registry.getSet();
        assertEquals(1, services.size(),
                "The registry should contain exactly 1 service");
        AndroidServiceEntry savedEntry = services.iterator().next();

        assertEquals(entry, savedEntry,
                "The saved entry should be equal to the original entry");
    }

    @Test
    void testGetInstanceNullContext() {
        assertThrows(NullPointerException.class, () -> AndroidServiceRegistry.getInstance(null),
                "Should throw NullPointerException when context is null");
    }

    @Test
    void testGetInstance() {
        Context mockContext = mock(Context.class);
        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockContext.getSharedPreferences(any(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putStringSet(any(), any())).thenReturn(mockEditor);

        AndroidServiceRegistry instance1 = AndroidServiceRegistry.getInstance(mockContext);
        AndroidServiceRegistry instance2 = AndroidServiceRegistry.getInstance(mockContext);

        assertNotNull(instance1, "Registry instance should not be null");
        assertSame(instance1, instance2, "Should return the same instance (singleton)");
    }
}
