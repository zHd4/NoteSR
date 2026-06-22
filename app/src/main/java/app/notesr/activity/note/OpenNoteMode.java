/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OpenNoteMode {
    EDIT(0),
    MARKDOWN_VIEW(1);

    private final int modeCode;

    public static OpenNoteMode fromCode(int code) {
        for (OpenNoteMode mode : values()) {
            if (mode.getModeCode() == code) {
                return mode;
            }
        }

        return null;
    }
}
