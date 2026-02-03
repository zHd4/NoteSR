/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum KeySetupMode {
    FIRST_RUN("first_run"),
    REGENERATION("regeneration");

    public final String mode;
}
