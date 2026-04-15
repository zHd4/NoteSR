/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

class AndroidServiceRegistryTest {

    private AndroidServiceRegistry registry;

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
        registry = new AndroidServiceRegistry();
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
    }

    @Test
    void testUnregister() {
        registry.register(TestService1.class);
        assertTrue(registry.isServiceRunning(TestService1.class),
                "Service 1 should be running after registration");
        
        registry.unregister(TestService1.class);
        assertFalse(registry.isServiceRunning(TestService1.class),
                "Service 1 should not be running after unregistration");
    }

    @Test
    void testGetSetReturnsACopy() {
        registry.register(TestService1.class);
        Set<Class<? extends Service>> services = registry.getSet();

        assertEquals(1, services.size(),
                "The set should contain 1 service");
        assertTrue(services.contains(TestService1.class),
                "The set should contain TestService1");

        services.add(TestService2.class);
        assertFalse(registry.isServiceRunning(TestService2.class),
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
}
