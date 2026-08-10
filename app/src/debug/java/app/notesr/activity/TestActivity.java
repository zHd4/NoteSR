/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.os.Bundle;

import lombok.Getter;
import lombok.Setter;

public class TestActivity extends ActivityBase {

    public static final String EXTRA_REQUIRES_SESSION = "requiresSession";

    @Setter
    private boolean requiresSessionValue = true;

    @Getter
    private boolean restartCalled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null && getIntent().hasExtra(EXTRA_REQUIRES_SESSION)) {
            requiresSessionValue = getIntent().getBooleanExtra(EXTRA_REQUIRES_SESSION,
                    true);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean requiresSession() {
        return requiresSessionValue;
    }

    @Override
    protected void restartApp() {
        // Mark restart as called and mock behavior
        restartCalled = true;
    }
}
