/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

import dagger.hilt.android.testing.HiltTestApplication;

public final class HiltTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(
            ClassLoader classLoader,
            String className,
            Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(
                classLoader,
                HiltTestApplication.class.getName(),
                context);
    }
}
