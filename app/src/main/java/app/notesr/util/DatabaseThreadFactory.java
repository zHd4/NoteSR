/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.util;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DatabaseThreadFactory implements ThreadFactory {
    private final ThreadFactory delegate = Executors.defaultThreadFactory();
    private final AtomicReference<Throwable> uncaughtException;
    private final String logTag;

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = delegate.newThread(task);

        thread.setUncaughtExceptionHandler((failedThread, exception) -> {
            uncaughtException.compareAndSet(null, exception);
            Log.e(logTag, "Uncaught exception in database operation", exception);
        });

        return thread;
    }
}