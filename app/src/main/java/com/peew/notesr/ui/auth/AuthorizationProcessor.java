package com.peew.notesr.ui.auth;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.ui.KeyRecoveryActivity;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.crypto.CryptoManager;

public class AuthorizationProcessor {
    private static final int MAX_ATTEMPTS = 3;
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;
    private final AuthActivity activity;
    private final String password;
    private int attempts = MAX_ATTEMPTS;

    public AuthorizationProcessor(AuthActivity activity, String password) {
        this.activity = activity;
        this.password = password;
    }

    public void proceed() {
        CryptoManager cryptoManager = CryptoManager.getInstance();
        TextView censoredPasswordView = activity.findViewById(R.id.censored_password_text_view);

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
