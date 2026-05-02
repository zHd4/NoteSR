/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecretsUpdateStatus {
    INITIALIZING("initializing", 0),
    MOVING_BLOBS_DATA("moving_blobs_data", 1),
    MOVING_DB_DATA("moving_db_data", 2),
    DONE("done", 3),
    FAILED("failed", -1);

    private final String status;
    private final int order;

    public boolean isBefore(SecretsUpdateStatus other) {
        return this.order < other.order;
    }

    public boolean isBeforeOrEqual(SecretsUpdateStatus other) {
        return this.order <= other.order;
    }
}
