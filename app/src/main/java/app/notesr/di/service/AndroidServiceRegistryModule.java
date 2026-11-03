/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.service.AndroidServiceRegistry;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the application-wide registry of running Android services.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class AndroidServiceRegistryModule {

    @Provides
    AndroidServiceRegistry provideAndroidServiceRegistry(
            @NonNull @ApplicationContext Context context) {
        return AndroidServiceRegistry.getInstance(context);
    }
}
