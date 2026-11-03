/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service.security;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.rotation.SecretsRotationService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the service responsible for rotating application security secrets.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class SecretsRotationServiceModule {

    @Provides
    SecretsRotationService provideSecretsRotationService(
            @NonNull @ApplicationContext Context context,
            @NonNull AppSecurityService appSecurityService) {
        return new SecretsRotationService(context, appSecurityService);
    }
}
