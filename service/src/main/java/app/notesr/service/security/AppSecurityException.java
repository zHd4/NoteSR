/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

public class AppSecurityException extends RuntimeException {
    public AppSecurityException(String message) {
        super(message);
    }

    public AppSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
