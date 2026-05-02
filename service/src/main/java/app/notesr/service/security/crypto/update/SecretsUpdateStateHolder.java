/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.crypto.update;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecretsUpdateStateHolder {

    private final Consumer<SecretsUpdateState> onUpdate;

    private SecretsUpdateState state = new SecretsUpdateState();

    public SecretsUpdateState getState() {
        return SecretsUpdateState.from(state);
    }

    public SecretsUpdateStateHolder setState(SecretsUpdateState newState) {
        if (newState == null) {
            return this;
        }

        state = SecretsUpdateState.from(newState);
        onUpdate.accept(state);

        return this;
    }
}
