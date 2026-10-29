/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di;

import android.content.Context;

import androidx.annotation.NonNull;
import dagger.hilt.android.qualifiers.ApplicationContext;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.rotation.SecretsRotationService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/**
 * Dagger Hilt module for providing security-related services.
 * <p>
 * This module provides dependencies for application security operations,
 * including authentication, encryption key management, and secret rotation.
 */
@Module
@InstallIn(SingletonComponent.class)
public class SecurityModule {

    /**
     * Provides the {@link CryptoManager} instance for cryptographic operations.
     *
     * @param context the application context
     * @return the singleton CryptoManager instance
     */
    @Provides
    CryptoManager provideCryptoManager(
            @NonNull @ApplicationContext Context context) {
        return CryptoManagerProvider.getInstance(context);
    }

    /**
     * Provides the {@link AppSecurityService} instance for security operations.
     *
     * @param context the application context
     * @param cryptoManager the crypto manager instance
     * @return the singleton AppSecurityService instance
     */
    @Provides
    AppSecurityService provideAppSecurityService(
            @NonNull @ApplicationContext Context context,
            @NonNull CryptoManager cryptoManager) {
        return new AppSecurityService(context, cryptoManager);
    }

    /**
     * Provides the {@link SecretsRotationService} instance for rotating encryption secrets.
     *
     * @param context the application context
     * @param appSecurityService the app security service instance
     * @return the singleton SecretsRotationService instance
     */
    @Provides
    SecretsRotationService provideSecretsRotationService(
            @NonNull @ApplicationContext Context context,
            @NonNull AppSecurityService appSecurityService) {
        return new SecretsRotationService(context, appSecurityService);
    }
}
