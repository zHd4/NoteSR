/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import android.content.Context;

import androidx.media3.exoplayer.ExoPlayer;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.ActivityScoped;

/**
 * Provides an activity-scoped media player using the current activity context.
 */
@Module
@InstallIn(ActivityComponent.class)
public final class ExoPlayerModule {

    @Provides
    @ActivityScoped
    ExoPlayer provideExoPlayer(@ActivityContext Context context) {
        return new ExoPlayer.Builder(context).build();
    }
}
