/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

public final class FilesTransactionException extends RuntimeException {
    public FilesTransactionException(String message) {
        super(message);
    }

    public FilesTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
