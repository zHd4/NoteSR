package com.peew.notesr.activity.security;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;

public class AuthActivityExtension {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;

    private final AuthActivity activity;
    private final StringBuilder passwordBuilder;

    private int attempts = MAX_ATTEMPTS;
    private String createdPassword;

    public AuthActivityExtension(AuthActivity activity, StringBuilder passwordBuilder) {
        this.activity = activity;
        this.passwordBuilder = passwordBuilder;
    }

    public void authorize() {
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

                showToastMessage(R.string.blocked);
                activity.startActivity(new Intent(App.getContext(), KeyRecoveryActivity.class));
            } else {
                try {
                    Thread.sleep(ON_WRONG_PASSWORD_DELAY_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                showToastMessage(String.format(
                        activity.getString(R.string.wrong_code_you_have_n_attempts),
                        attempts));
            }

            resetPassword();
        } else {
            censoredPasswordView.setText("");
            activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
        }
    }

    public void createPassword() {
        String password = proceedPasswordSetting();

        if (password != null) {
            Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);

            setupKeyActivityIntent.putExtra("mode", SetupKeyActivity.FIRST_RUN_MODE);
            setupKeyActivityIntent.putExtra("password", password);

            activity.startActivity(setupKeyActivityIntent);
        }
    }

    public void recoverKey() {
        String password = proceedPasswordSetting();

        if (password != null) {
            String hexKey = activity.getIntent().getStringExtra("hexKey");

            try {
                if (hexKey == null) throw new Exception("Missing hex key");

                App.getAppContainer()
                        .getCryptoManager()
                        .applyNewKey(CryptoTools.hexToCryptoKey(hexKey, password));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
        }
    }

    public void changePassword() {
        String password = proceedPasswordSetting();

        if (password != null) {
            try {
                App.getAppContainer().getCryptoManager().changePassword(password);

                showToastMessage(R.string.updated);
                activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String proceedPasswordSetting() {
        String password = passwordBuilder.toString();
        TextView topLabel = activity.findViewById(R.id.auth_top_label);

        if (createdPassword == null) {
            if (passwordBuilder.length() >= MIN_PASSWORD_LENGTH) {
                createdPassword = password;
                topLabel.setText(activity.getString(R.string.repeat_access_code));
            } else {
                showToastMessage(String.format(
                        activity.getString(R.string.minimum_password_length_is_n),
                        MIN_PASSWORD_LENGTH));
            }
        } else {
            if (password.equals(createdPassword)) {
                return password;
            } else {
                showToastMessage(R.string.code_not_match);
            }
        }

        resetPassword();
        return null;
    }

    private void resetPassword() {
        TextView censoredPasswordView = activity.findViewById(R.id.censored_password_text_view);

        censoredPasswordView.setText("");
        passwordBuilder.setLength(0);
    }

    private void showToastMessage(int stringResId) {
        showToastMessage(activity.getString(stringResId));
    }

    private void showToastMessage(String text) {
        activity.showToastMessage(text, Toast.LENGTH_SHORT);
    }
}
