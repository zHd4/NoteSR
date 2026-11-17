/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import android.widget.TextView;

import app.notesr.R;
import app.notesr.core.util.SecureStringBuilder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordHelper {

    private final AppPasswordActivity activity;
    private final SecureStringBuilder passwordBuilder;

    public void resetPassword() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);
        censoredPasswordView.setText("");
        passwordBuilder.wipe();
    }
}
