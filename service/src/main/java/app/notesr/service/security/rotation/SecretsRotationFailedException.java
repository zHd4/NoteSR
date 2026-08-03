/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.rotation;

public final class SecretsRotationFailedException extends RuntimeException {

    public SecretsRotationFailedException(String message) {
        super(message);
    }

    public SecretsRotationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
