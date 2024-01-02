package com.peew.notesr.activities.auth;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activities.KeyRecoveryActivity;
import com.peew.notesr.activities.MainActivity;
import com.peew.notesr.crypto.CryptoManager;

public class AuthorizationProcessor {
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;
    private final AuthActivity activity;
    private final String password;
    private final TextView censoredPasswordView;
    private final CryptoManager cryptoManager;
    private int attempts;

    public AuthorizationProcessor(AuthActivity activity, String password,
                                  TextView censoredPasswordView, CryptoManager cryptoManager,
                                  int attempts) {
        this.activity = activity;
        this.password = password;
        this.censoredPasswordView = censoredPasswordView;
        this.cryptoManager = cryptoManager;
        this.attempts = attempts;
    }

    public void proceed() {
        if (password.isEmpty()) {
            String enterCodeMessage = activity.getString(R.string.enter_the_code);
            activity.showToastMessage(enterCodeMessage, Toast.LENGTH_SHORT);
            return;
        }

        if (!cryptoManager.configure(password)) {
            attempts--;

            if (attempts == 0) {
                cryptoManager.block();

                activity.resetPassword(activity.getString(R.string.blocked));
                activity.startActivity(new Intent(App.getContext(), KeyRecoveryActivity.class));
            } else {
                try {
                    Thread.sleep(ON_WRONG_PASSWORD_DELAY_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                String messageFormat = activity.getString(R.string.wrong_code_you_have_n_attempts);
                activity.resetPassword(String.format(messageFormat, attempts));
            }
        } else {
            censoredPasswordView.setText("");
            activity.startActivity(new Intent(App.getContext(), MainActivity.class));
        }
    }
}
