/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum KeySetupMode {
    FIRST_RUN("first_run"),
    REGENERATION("regeneration");

    private final String mode;

    public static KeySetupMode fromValue(String value) {
        for (KeySetupMode mode : values()) {
            if (mode.mode.equals(value)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown key setup mode: " + value);
    }
}
