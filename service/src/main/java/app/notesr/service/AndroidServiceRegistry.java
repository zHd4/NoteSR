/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A registry that keeps track of currently running {@link Service} components.
 * This is useful for checking if a background task is already in progress.
 * All Android services should be registered here.
 * <p>
 * The registry persists the state of running services to {@link SharedPreferences}
 * to allow recovery after application restart.
 */
public final class AndroidServiceRegistry {

    private static final String PREF_NAME = "android_services_registry_prefs";
    private static final String RUNNING_SERVICES_PREF = "current_running_services";

    /**
     * The singleton instance of the {@link AndroidServiceRegistry}.
     */
    private static AndroidServiceRegistry instance;

    private final SharedPreferences prefs;

    /**
     * A thread-safe set of service classes that are currently active.
     */
    private Set<AndroidServiceEntry> runningServices =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Initializes a new instance of the {@link AndroidServiceRegistry}.
     *
     * @param prefs the {@link SharedPreferences} to use for persistence
     */
    AndroidServiceRegistry(SharedPreferences prefs) {
        this.prefs = prefs;
        this.runningServices = getServicesFromPrefs();
    }

    /**
     * Returns the singleton instance of the {@link AndroidServiceRegistry}.
     *
     * @param context the context used to initialize the registry if needed
     * @return the singleton instance
     */
    public static AndroidServiceRegistry getInstance(Context context) {
        synchronized (AndroidServiceRegistry.class) {
            if (instance == null) {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME,
                        Context.MODE_PRIVATE);

                instance = new AndroidServiceRegistry(prefs);
            }
        }

        return instance;
    }

    /**
     * Registers a service as running with auto-start set to {@code false}.
     *
     * @param serviceClass the class of the service to register
     */
    public void register(Class<? extends Service> serviceClass) {
        register(serviceClass, false);
    }

    /**
     * Registers a service as running.
     *
     * @param serviceClass the class of the service to register
     * @param autoStart    whether the service should be automatically started on next boot
     */
    public void register(Class<? extends Service> serviceClass, boolean autoStart) {
        AndroidServiceEntry entry = new AndroidServiceEntry(
                serviceClass,
                serviceClass.getSimpleName(),
                autoStart
        );

        runningServices.add(entry);
        saveServices(runningServices);
    }

    /**
     * Unregisters a service, indicating it is no longer running.
     *
     * @param serviceClass the class of the service to unregister
     */
    public void unregister(Class<? extends Service> serviceClass) {
        runningServices.removeIf(entry ->
                entry.getServiceClass().equals(serviceClass));

        saveServices(runningServices);
    }

    /**
     * Checks whether a specific service is currently running.
     *
     * @param serviceClass the class of the service to check
     * @return {@code true} if the service is registered as running, {@code false} otherwise
     */
    public boolean isServiceRunning(Class<? extends Service> serviceClass) {
        Optional<AndroidServiceEntry> entry = runningServices.stream()
                .filter(s -> s.getServiceClass().equals(serviceClass))
                .findAny();

        return entry.isPresent();
    }

    /**
     * Returns a copy of the set of currently running service entries.
     *
     * @return a new set containing all registered {@link AndroidServiceEntry} objects
     */
    public Set<AndroidServiceEntry> getSet() {
        return new HashSet<>(runningServices);
    }

    /**
     * Saves the current set of running services to {@link SharedPreferences}.
     * Each service entry is serialized to JSON before being stored.
     *
     * @param services the set of {@link AndroidServiceEntry} objects to save
     * @throws RuntimeException if a service entry fails to serialize
     */
    private void saveServices(Set<AndroidServiceEntry> services) {
        Set<String> servicesJson = new HashSet<>();

        for (AndroidServiceEntry entry : services) {
            try {
                servicesJson.add(entry.toJson());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize service", e);
            }
        }

        prefs.edit().putStringSet(RUNNING_SERVICES_PREF, servicesJson).apply();
    }

    /**
     * Loads the set of running services from {@link SharedPreferences}.
     * Service entries are deserialized from JSON and added to the registry.
     *
     * @throws RuntimeException if a service entry fails to deserialize
     * @return a new set containing all loaded {@link AndroidServiceEntry} objects
     */
    private Set<AndroidServiceEntry> getServicesFromPrefs() {
        Set<String> servicesJson = prefs.getStringSet(RUNNING_SERVICES_PREF, null);
        Set<AndroidServiceEntry> services = new HashSet<>();

        if (servicesJson != null) {
            for (String serviceJson : servicesJson) {
                try {
                    AndroidServiceEntry entry = AndroidServiceEntry.fromJson(serviceJson);
                    services.add(entry);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to deserialize service", e);
                }
            }
        }

        return services;
    }
}
