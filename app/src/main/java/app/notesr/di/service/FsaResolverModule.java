/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service;

import androidx.annotation.NonNull;

import app.notesr.activity.FsaResolver;
import app.notesr.service.AndroidServiceRegistry;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the resolver for associating running services with their activities.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class FsaResolverModule {

    @Provides
    FsaResolver provideFsaResolver(@NonNull AndroidServiceRegistry registry) {
        return new FsaResolver(registry);
    }
}
