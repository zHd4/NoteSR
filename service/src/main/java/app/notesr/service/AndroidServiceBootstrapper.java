/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import app.notesr.core.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

/**
 * Responsible for orchestrating the lifecycle of Android services within the application.
 * <p>
 * This class handles the initialization and execution of services registered in the
 * {@link AndroidServiceRegistry}, distinguishing between those that can run immediately
 * and those that require user authentication (and thus access to {@link CryptoSecrets}).
 */
@RequiredArgsConstructor
public final class AndroidServiceBootstrapper {

    private final AndroidServiceRegistry registry;

    /**
     * Starts all auto-start services that do not require authentication.
     *
     * @param context the Android application context used to initialize services
     */
    public void startServicesPreAuth(Context context) {
        registry.getSet().stream()
                .filter(AndroidServiceEntry::isAutoStart)
                .filter(serviceEntry -> !serviceEntry.isRequiresAuth())
                .forEach(serviceEntry ->
                        startService(context, serviceEntry, null));
    }

    /**
     * Starts all auto-start services that require authentication.
     *
     * @param context the Android application context used to initialize services
     * @param secrets the cryptographic secrets required for authenticated services
     * @throws NullPointerException if secrets is null
     */
    public void startServicesPostAuth(Context context, CryptoSecrets secrets) {
        requireNonNull(secrets, "Secrets are null");
        registry.getSet().stream()
                .filter(AndroidServiceEntry::isAutoStart)
                .filter(AndroidServiceEntry::isRequiresAuth)
                .forEach(serviceEntry ->
                        startService(context, serviceEntry, secrets));
    }

    /**
     * Instantiates the service starter and executes the startup logic for a specific entry.
     *
     * @param context      the Android application context
     * @param serviceEntry the metadata for the service to be started
     * @param secrets      optional cryptographic secrets for authenticated services
     * @throws UnsupportedOperationException if the service is not marked as auto-start
     * @throws RuntimeException if instantiation or startup of the service fails
     */
    private void startService(
            Context context,
            AndroidServiceEntry serviceEntry,
            CryptoSecrets secrets
    ) {
        requireNonNull(serviceEntry, "Service entry is null");

        String serviceCanonicalName = requireNonNull(serviceEntry.getServiceClass(),
                "Service class is null").getCanonicalName();

        if (!serviceEntry.isAutoStart()) {
            throw new UnsupportedOperationException(
                    "Service " + serviceCanonicalName + " is not auto-start");
        }

        Class<? extends AndroidServiceStarter> starterClass = requireNonNull(
                serviceEntry.getStarterClass(),
                "Starter class of service " + serviceCanonicalName + " is null");

        try {
            AndroidServiceStarter starter = starterClass.getDeclaredConstructor().newInstance();
            String payload = serviceEntry.getPayload();

            if (payload != null) {
                starter.start(context, secrets, payload);
            } else {
                starter.start(context);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Starter class of service " + serviceCanonicalName
                    + " does not have a default constructor", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "Failed to instantiate starter class of service " + serviceCanonicalName, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start service " + serviceCanonicalName, e);
        }
    }
}
