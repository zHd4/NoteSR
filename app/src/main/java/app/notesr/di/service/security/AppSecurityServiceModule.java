/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.service.security;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.service.security.AppSecurityService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the application security service backed by the crypto manager.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class AppSecurityServiceModule {

    @Provides
    AppSecurityService provideAppSecurityService(
            @NonNull @ApplicationContext Context context,
            @NonNull CryptoManager cryptoManager) {
        return new AppSecurityService(context, cryptoManager);
    }
}
