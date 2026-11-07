/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class DatabaseUtils {
    private static final long DATABASE_BARRIER_TIMEOUT_SECONDS = 5;

    public static Throwable awaitDatabaseOperations(
            ExecutorService databaseExecutor,
            AtomicReference<Throwable> uncaughtDatabaseException) {
        CountDownLatch barrier = new CountDownLatch(1);
        databaseExecutor.execute(barrier::countDown);

        try {
            if (!barrier.await(DATABASE_BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for database operations");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for database operations", e);
        }

        return uncaughtDatabaseException.get();
    }
}
