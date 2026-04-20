/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class AndroidServiceRegistryTest {

    private AndroidServiceRegistry registry;
    private SharedPreferences.Editor editor;

    private static class TestService1 extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    private static class TestService2 extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
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

        registry.register(TestService1.class);
        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running after registration");
        assertFalse(registry.isServiceRunning(TestService2.class),
                "Service 2 should not be running");

        verify(editor, times(1))
                .putStringSet(eq("auto_start_services"), any());
        verify(editor, times(1)).apply();
    }

    @Test
    void testUnregister() {
        registry.register(TestService1.class);
        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running after registration");

        registry.unregister(TestService1.class);
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running after unregistration");

        verify(editor, times(2)).apply();
    }

    @Test
    void testGetSetReturnsACopy() {
        registry.register(TestService1.class);
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
        registry.register(TestService1.class);
        registry.register(TestService2.class);

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
        registry.register(TestService1.class, true);
        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running");

        AndroidServiceEntry entry = registry.getSet().iterator().next();
        assertTrue(entry.isAutoStart(),
                "Service should have autoStart set to true");
    }

    @Test
    void testLoadFromPrefs() throws Exception {
        AndroidServiceEntry entry = new AndroidServiceEntry(
                TestService1.class,
                TestService1.class.getName(),
                true
        );

        Set<String> jsonSet = new HashSet<>();
        jsonSet.add(entry.toJson());

        SharedPreferences mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getStringSet(eq("auto_start_services"), eq(null)))
                .thenReturn(jsonSet);

        AndroidServiceRegistry newRegistry = new AndroidServiceRegistry(mockPrefs);
        assertTrue(newRegistry.isServiceRunning(TestService1.class),
                "Service 1 should be running after loading from prefs");

        AndroidServiceEntry loadedEntry = newRegistry.getSet().iterator().next();
        assertTrue(loadedEntry.isAutoStart(),
                "Loaded service should have autoStart set to true");
    }
}
