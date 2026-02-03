/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.content.Intent;

import java.util.List;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class StartupIntentResolver {
    private final List<Supplier<Intent>> intentSuppliers;
    private final Intent defaultIntent;

    public Intent resolve() {
        for (Supplier<Intent> supplier : intentSuppliers) {
            Intent intent = supplier.get();

            if (intent != null) {
                return intent;
            }
        }

        return defaultIntent;
    }
}
