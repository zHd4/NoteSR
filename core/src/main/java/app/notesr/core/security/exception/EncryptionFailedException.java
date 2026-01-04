/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EncryptionFailedException extends Exception {
    public EncryptionFailedException(Throwable cause) {
        super(cause);
    }
}
