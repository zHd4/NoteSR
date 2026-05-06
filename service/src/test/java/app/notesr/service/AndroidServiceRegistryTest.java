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

class AndroidServiceRegistryTest {

    private AndroidServiceRegistry registry;
    private SharedPreferences.Editor editor;

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
                .build());
        registry.register(AndroidServiceEntry.builder()
                .serviceClass(TestService2.class)
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
                .autoStart(true)
                .build();
        AndroidServiceEntry noAutoStartEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService2.class)
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
                .serviceName("Test Service 1")
                .payload("initial payload")
                .state("initial state")
                .build();

        registry.register(entry);

        AndroidServiceEntry updatedEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
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
                .serviceName("Test Service 1")
                .payload("initial payload")
                .state("initial state")
                .build();

        registry.register(entry);

        AndroidServiceEntry updatedEntry = AndroidServiceEntry.builder()
                .serviceClass(TestService1.class)
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
                .state("new state")
                .build();

        registry.updateEntry(entry);
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running after updating non-existent entry");
        verify(editor, times(2)).apply();
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
