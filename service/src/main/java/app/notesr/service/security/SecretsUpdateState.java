/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

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
public final class SecretsUpdateState implements Serializable {

    @Getter
    private SecretsUpdateStatus status;

    @Getter
    private String transactionId;

    public SecretsUpdateState setStatus(SecretsUpdateStatus status) {
        this.status = status;
        return this;
    }

    public SecretsUpdateState setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public static SecretsUpdateState from(SecretsUpdateState state) {
        if (state == null) {
            return null;
        }

        return new SecretsUpdateState(state.getStatus(), state.getTransactionId());
    }
}
