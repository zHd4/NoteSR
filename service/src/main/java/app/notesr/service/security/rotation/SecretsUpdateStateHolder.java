/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.rotation;

import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecretsUpdateStateHolder {

    private final Consumer<SecretsRotationState> onUpdate;

    private SecretsRotationState state = new SecretsRotationState();

    public SecretsRotationState getState() {
        return SecretsRotationState.from(state);
    }

    public SecretsUpdateStateHolder setState(SecretsRotationState newState) {
        if (newState == null) {
            return this;
        }

        state = SecretsRotationState.from(newState);
        onUpdate.accept(state);

        return this;
    }
}
