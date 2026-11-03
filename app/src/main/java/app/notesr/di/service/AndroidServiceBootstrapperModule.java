/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service;

import androidx.annotation.NonNull;

import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.AndroidServiceRegistry;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the bootstrapper that starts and manages application Android services.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class AndroidServiceBootstrapperModule {

    @Provides
    AndroidServiceBootstrapper provideAndroidServiceBootstrapper(
            @NonNull AndroidServiceRegistry registry) {
        return new AndroidServiceBootstrapper(registry);
    }
}
