/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import app.notesr.data.AppDatabase;

/**
 * Interface for managing database instances and the global database provider state
 * during secrets updates.
 */
public interface DatabaseManager {
    AppDatabase getDatabase(String name, byte[] key);
    void closeProvider();
    void reinitProvider(byte[] key);
    boolean isDbAvailable(AppDatabase db);
}
