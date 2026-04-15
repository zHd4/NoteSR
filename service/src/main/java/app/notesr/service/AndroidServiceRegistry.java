/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import android.app.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * A registry that keeps track of currently running {@link Service} components.
 * This is useful for checking if a background task is already in progress.
 * All Android services should be registered here.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AndroidServiceRegistry {

    /**
     * The singleton instance of the {@link AndroidServiceRegistry}.
     */
    private static AndroidServiceRegistry instance;

    /**
     * A thread-safe set of service classes that are currently active.
     */
    private final Set<Class<? extends Service>> runningServices =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Returns the singleton instance of the {@link AndroidServiceRegistry}.
     *
     * @return the singleton instance
     */
    public static AndroidServiceRegistry getInstance() {
        synchronized (AndroidServiceRegistry.class) {
            if (instance == null) {
                instance = new AndroidServiceRegistry();
            }
        }

        return instance;
    }

    /**
     * Registers a service as running.
     *
     * @param serviceClass the class of the service to register
     */
    public void register(Class<? extends Service> serviceClass) {
        runningServices.add(serviceClass);
    }

    /**
     * Unregisters a service, indicating it is no longer running.
     *
     * @param serviceClass the class of the service to unregister
     */
    public void unregister(Class<? extends Service> serviceClass) {
        runningServices.remove(serviceClass);
    }

    /**
     * Checks whether a specific service is currently running.
     *
     * @param serviceClass the class of the service to check
     * @return {@code true} if the service is registered as running, {@code false} otherwise
     */
    public boolean isServiceRunning(Class<? extends Service> serviceClass) {
        return runningServices.contains(serviceClass);
    }

    /**
     * Returns a copy of the set of currently running service classes.
     *
     * @return a new set containing all registered running service classes
     */
    public Set<Class<? extends Service>> getSet() {
        return new HashSet<>(runningServices);
    }
}
