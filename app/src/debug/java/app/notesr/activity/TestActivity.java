/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.os.Bundle;

import lombok.Setter;

public class TestActivity extends ActivityBase {

    @Setter
    private boolean isSessionRequired = true; // Default value

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean requiresSession() {
        return isSessionRequired;
    }
}
