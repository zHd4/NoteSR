/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.di.util;

import android.app.Activity;
import android.content.Context;

import app.notesr.activity.DialogFactory;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.ActivityScoped;

/**
 * Provides a dialog factory scoped to the current activity.
 */
@Module
@InstallIn(ActivityComponent.class)
public final class DialogFactoryModule {

    @Provides
    @ActivityScoped
    DialogFactory provideDialogFactory(@ActivityContext Context context) {
        return new DialogFactory((Activity) context);
    }
}
