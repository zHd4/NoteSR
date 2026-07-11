/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import android.app.Service;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.security.crypto.ValueEncryptor;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.EncryptionFailedException;

/**
 * Base abstract class for Android services within the NoteSR ecosystem.
 * Provides common functionality for lifecycle management, service registration,
 * and payload serialization/encryption.
 */
public abstract class AndroidService extends Service {

    /** @serialData Checks if the class is instantiated directly as AndroidService. */
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate() {
        super.onCreate();

        if (getClass() == AndroidService.class) {
            throw new IllegalStateException(
                    "AndroidService is base class and cannot be started directly"
            );
        }
    }
    /**
     * Unregisters the service from {@link AndroidServiceRegistry} upon destruction.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        AndroidServiceRegistry.getInstance(getApplicationContext()).unregister(getClass());
    }

    /**
     * Registers this service instance in the {@link AndroidServiceRegistry}.
     * This method should be called when the service is fully initialized and ready to operate.
     *
     * @param payload The data payload associated with the service registration.
     * @param state   The current operational state of the service.
     */
    protected void register(String payload, String state) {
        AndroidServiceRegistry.getInstance(getApplicationContext())
                .register(getEntry(payload, state));
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param mapper  The {@link ObjectMapper} to use for serialization.
     * @param obj The object to serialize.
     * @return A JSON string representation of the object.
     * @throws RuntimeException if serialization fails.
     */
    protected String getPlainJson(ObjectMapper mapper, Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    /**
     * Serializes an object to JSON and encrypts it using AES-GCM.
     *
     * @param mapper  The {@link ObjectMapper} to use for serialization.
     * @param obj The object to serialize and encrypt.
     * @param secrets The {@link CryptoSecrets} containing the encryption key.
     * @return An encrypted string representation of the object.
     * @throws RuntimeException if serialization or encryption fails.
     */
    protected String getEncryptedJson(
            ObjectMapper mapper,
            Object obj,
            CryptoSecrets secrets
    ) {
        try {
            var payloadJson = mapper.writeValueAsString(obj);
            var encryptor = new ValueEncryptor(AesCryptorFactory.createAesGcmCryptor(secrets));

            return encryptor.encrypt(payloadJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object", e);
        } catch (EncryptionFailedException e) {
            throw new RuntimeException("Failed to encrypt object", e);
        }
    }

    /**
     * Creates a builder for {@link AndroidServiceEntry} pre-populated with this service's details.
     *
     * @param starterClass The class responsible for starting this service.
     * @return An {@link AndroidServiceEntry.AndroidServiceEntryBuilder} instance.
     */
    protected AndroidServiceEntry.AndroidServiceEntryBuilder entryBuilder(
            Class<? extends AndroidServiceStarter> starterClass
    ) {
        return AndroidServiceEntry.builder()
                .serviceClass(getClass())
                .starterClass(starterClass)
                .serviceName(getClass().getSimpleName());
    }

    /**
     * Provides the registration entry for this service.
     *
     * @return The {@link AndroidServiceEntry} defining this service's identity and metadata.
     */
    @NonNull
    protected abstract AndroidServiceEntry getEntry(String payload, String state);
}
