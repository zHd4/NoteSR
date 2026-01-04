/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.importer;

public class ImportFailedException extends RuntimeException {

    public ImportFailedException(String message) {
        super(message);
    }

    public ImportFailedException(Exception e) {
        super(e);
    }
}
