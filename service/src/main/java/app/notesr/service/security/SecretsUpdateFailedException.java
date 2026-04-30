/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

public final class SecretsUpdateFailedException extends RuntimeException {

    public SecretsUpdateFailedException(String message) {
        super(message);
    }

    public SecretsUpdateFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
