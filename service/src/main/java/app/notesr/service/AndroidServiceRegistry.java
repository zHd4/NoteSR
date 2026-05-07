/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A registry that keeps track of currently running {@link AndroidService} components.
 * This is useful for checking if a background task is already in progress.
 * All Android services should be registered here.
 * <p>
 * The registry persists the state of running services to {@link SharedPreferences}
 * to allow recovery after application restart.
 */
public final class AndroidServiceRegistry {

    private static final String PREF_NAME = "android_services_registry_prefs";
    private static final String RUNNING_SERVICES_PREF = "current_running_services";

    private static AndroidServiceRegistry instance;

    private final SharedPreferences prefs;
    private final Map<Class<? extends AndroidService>, AndroidServiceEntry> runningServices;

    /**
     * Initializes a new instance of the {@link AndroidServiceRegistry}.
     * This constructor only for testing purposes and internal use.
     * Should not be used in production.
     *
     * @param prefs the {@link SharedPreferences} to use for persistence
     * @throws NullPointerException if prefs is null
     */
    AndroidServiceRegistry(SharedPreferences prefs) {
        this.prefs = requireNonNull(prefs, "Preferences cannot be null");
        this.runningServices = getServicesFromPrefs();
        saveServices(runningServices.values());
    }

    /**
     * Returns the singleton instance of the {@link AndroidServiceRegistry}.
     *
     * @param context the context used to initialize the registry if needed
     * @return the singleton instance
     * @throws NullPointerException if context is null
     */
    public static AndroidServiceRegistry getInstance(Context context) {
        requireNonNull(context, "Context cannot be null");
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
     * Registers a service as running in the registry if it is not already registered.
     *
     * @param serviceEntry the {@link AndroidServiceEntry} representing the service to register
     * @throws NullPointerException if the service entry or its required fields are null
     */
    public void register(AndroidServiceEntry serviceEntry) {
        isEntryValid(serviceEntry);

        synchronized (runningServices) {
            if (isServiceRunning(serviceEntry.getServiceClass())) {
                updateEntry(serviceEntry);
                return;
            }

            runningServices.put(serviceEntry.getServiceClass(), serviceEntry);
            saveServices(runningServices.values());
        }
    }

    /**
     * Unregisters a service, indicating it is no longer running.
     *
     * @param serviceClass the class of the service to unregister
     * @throws NullPointerException if serviceClass is null
     */
    public void unregister(Class<? extends AndroidService> serviceClass) {
        requireNonNull(serviceClass, "Service class cannot be null");
        synchronized (runningServices) {
            runningServices.remove(serviceClass);
            saveServices(runningServices.values());
        }
    }

    /**
     * Checks whether a specific service is currently running.
     *
     * @param serviceClass the class of the service to check
     * @return {@code true} if the service is registered as running, {@code false} otherwise
     * @throws NullPointerException if serviceClass is null
     */
    public boolean isServiceRunning(Class<? extends AndroidService> serviceClass) {
        requireNonNull(serviceClass, "Service class cannot be null");
        return runningServices.containsKey(serviceClass);
    }

    /**
     * Updates the state and payload of an existing service entry and persists the change.
     * This method finds the registered service matching the class of the provided
     * entry and synchronizes its state.
     *
     * @param entry the {@link AndroidServiceEntry} containing the updated state
     * @throws NullPointerException if the entry or its required fields are null
     * @throws IllegalArgumentException if the service class, starter class, or service name
     *         mismatch
     */
    public void updateEntry(AndroidServiceEntry entry) {
        isEntryValid(entry);

        synchronized (runningServices) {
            AndroidServiceEntry existingEntry = runningServices.get(entry.getServiceClass());

            if (existingEntry != null) {
                if (existingEntry.getServiceClass() != entry.getServiceClass()) {
                    throw new IllegalArgumentException("Service class mismatch");
                }

                if (existingEntry.getStarterClass() != entry.getStarterClass()) {
                    throw new IllegalArgumentException("Starter class mismatch");
                }

                if (!existingEntry.getServiceName().equals(entry.getServiceName())) {
                    throw new IllegalArgumentException("Service name mismatch");
                }

                existingEntry.setPayload(entry.getPayload());
                existingEntry.setState(entry.getState());
            }

            saveServices(runningServices.values());
        }
    }

    /**
     * Returns a copy of the set of currently running service entries.
     * <p>
     * This method is thread-safe.
     *
     * @return a new set containing all registered {@link AndroidServiceEntry} objects
     */
    public Set<AndroidServiceEntry> getSet() {
        synchronized (runningServices) {
            return new HashSet<>(runningServices.values());
        }
    }

    /**
     * Retrieves the set of running services from {@link SharedPreferences}.
     * Service entries are deserialized from JSON and added to the registry.
     *
     * @throws RuntimeException if a service entry fails to deserialize
     * @return a new thread-safe set containing all loaded {@link AndroidServiceEntry} objects
     */
    private Map<Class<? extends AndroidService>, AndroidServiceEntry> getServicesFromPrefs() {
        Set<String> servicesJson = prefs.getStringSet(RUNNING_SERVICES_PREF, null);
        Map<Class<? extends AndroidService>, AndroidServiceEntry> services =
                Collections.synchronizedMap(new HashMap<>());

        if (servicesJson != null) {
            for (String serviceJson : servicesJson) {
                try {
                    AndroidServiceEntry entry = AndroidServiceEntry.fromJson(serviceJson);

                    if (!entry.isAutoStart()) {
                        continue;
                    }

                    services.put(entry.getServiceClass(), entry);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to deserialize service", e);
                }
            }
        }

        return services;
    }

    /**
     * Saves the current set of running services to {@link SharedPreferences}.
     * Each service entry is serialized to JSON before being stored.
     *
     * @param services the set of {@link AndroidServiceEntry} objects to save
     * @throws RuntimeException if a service entry fails to serialize
     */
    private void saveServices(Collection<AndroidServiceEntry> services) {
        Set<String> servicesJson = new HashSet<>();

        synchronized (runningServices) {
            for (AndroidServiceEntry entry : services) {
                try {
                    servicesJson.add(entry.toJson());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize service", e);
                }
            }
        }

        prefs.edit().putStringSet(RUNNING_SERVICES_PREF, servicesJson).apply();
    }

    private void isEntryValid(AndroidServiceEntry entry) {
        requireNonNull(entry, "Entry cannot be null");
        requireNonNull(entry.getServiceClass(), "Service class cannot be null");
        requireNonNull(entry.getStarterClass(), "Starter class cannot be null");
        requireNonNull(entry.getServiceName(), "Service name cannot be null");
    }
}
