/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.exporter;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExportStatus {
    INITIALIZING("initializing"),
    EXPORTING_DATA("exporting_data"),
    ENCRYPTING_DATA("encrypting_data"),
    WIPING_TEMP_DATA("wiping_temp_data"),
    DONE("done"),
    CANCELLING("cancelling"),
    CANCELED("canceled"),
    ERROR("error");

    private final String status;
}
