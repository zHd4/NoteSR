/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.security.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class DecryptionFailedException extends Exception {
    public DecryptionFailedException(Throwable cause) {
        super(cause);
    }
}
