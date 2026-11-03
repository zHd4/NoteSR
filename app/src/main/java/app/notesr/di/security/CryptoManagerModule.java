/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.security;

import android.content.Context;

import androidx.annotation.NonNull;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Provides the crypto manager initialized with the application context.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class CryptoManagerModule {

    @Provides
    CryptoManager provideCryptoManager(
            @NonNull @ApplicationContext Context context) {
        return CryptoManagerProvider.getInstance(context);
    }
}
