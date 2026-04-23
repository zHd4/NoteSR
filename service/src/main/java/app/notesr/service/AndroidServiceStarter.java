/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service;

import android.content.Context;

import app.notesr.core.security.dto.CryptoSecrets;

/**
 * Interface defining the contract for starting background services within the Android environment.
 * Provides methods for both simple service initialization and authenticated startup
 * with encrypted data.
 */
public interface AndroidServiceStarter {

    void start(Context context) throws Exception;
    void start(Context context, CryptoSecrets secrets, String payload) throws Exception;
}
