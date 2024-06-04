package com.peew.notesr.activity;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;

public class AuthActivityHelper {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;

    private final AuthActivity activity;
    private final StringBuilder passwordBuilder;

    private int attempts = MAX_ATTEMPTS;
    private String createdPassword;

    public AuthActivityHelper(AuthActivity activity, StringBuilder passwordBuilder) {
        this.activity = activity;
        this.passwordBuilder = passwordBuilder;
    }

    public void proceedAuth() {
        String password = passwordBuilder.toString();

        CryptoManager cryptoManager = App.getAppContainer().getCryptoManager();
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

    public void proceedPasswordCreation() {
        String password = setPassword();

        if (password != null) {
            Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);

            setupKeyActivityIntent.putExtra("mode", SetupKeyActivity.FIRST_RUN_MODE);
            setupKeyActivityIntent.putExtra("password", password);

            activity.startActivity(setupKeyActivityIntent);
        }
    }

    public void proceedKeyRecovery() {
        String password = setPassword();

        if (password != null) {
            String hexKey = activity.getIntent().getStringExtra("hex-key");

            try {
                if (hexKey == null) throw new Exception("Missing hex-key");

                App.getAppContainer()
                        .getCryptoManager()
                        .applyNewKey(CryptoTools.hexToCryptoKey(hexKey, password));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(App.getContext(), MainActivity.class));
        }
    }

    public void proceedPasswordChanging() {
        String password = setPassword();

        if (password != null) {
            try {
                App.getAppContainer().getCryptoManager().changePassword(password);
                activity.resetPassword(activity.getString(R.string.updated));

                activity.startActivity(new Intent(App.getContext(), MainActivity.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String setPassword() {
        String password = passwordBuilder.toString();

        TextView topLabel = activity.findViewById(R.id.auth_top_label);
        TextView censoredPasswordView = activity.findViewById(R.id.censored_password_text_view);

        if (createdPassword == null) {
            if (passwordBuilder.length() >= MIN_PASSWORD_LENGTH) {
                createdPassword = password;
                passwordBuilder.setLength(0);

                censoredPasswordView.setText("");
                topLabel.setText(activity.getString(R.string.repeat_access_code));

                return password;
            } else {
                activity.resetPassword(String.format(
                        activity.getString(R.string.minimum_password_length_is_n),
                        MIN_PASSWORD_LENGTH));
            }
        } else {
            if (!password.equals(createdPassword)) {
                activity.resetPassword(activity.getString(R.string.code_not_match));
                return null;
            }

            censoredPasswordView.setText("");
            return password;
        }

        return null;
    }
}
