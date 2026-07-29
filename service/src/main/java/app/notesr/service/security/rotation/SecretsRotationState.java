/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.rotation;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public final class SecretsRotationState implements Serializable {

    @Getter
    private SecretsRotationStatus status;

    @Getter
    private String transactionId;

    public SecretsRotationState setStatus(SecretsRotationStatus status) {
        this.status = status;
        return this;
    }

    public SecretsRotationState setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public static SecretsRotationState from(SecretsRotationState state) {
        if (state == null) {
            return null;
        }

        return new SecretsRotationState(state.getStatus(), state.getTransactionId());
    }
}
