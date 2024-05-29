package com.peew.notesr.ui.auth;

import android.content.Intent;
import android.widget.TextView;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoTools;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.setup.SetupKeyActivity;

public class PasswordSetupProcessor {
    private static final int MIN_PASSWORD_LENGTH = 4;
    private final AuthActivity activity;
    private String password;
    private final StringBuilder passwordBuilder;

    public PasswordSetupProcessor(AuthActivity activity, StringBuilder passwordBuilder) {
        this.activity = activity;
        this.passwordBuilder = passwordBuilder;
    }

    public void proceedPasswordCreation() {
        if (setPassword()) {
            Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);
            setupKeyActivityIntent.putExtra("mode", SetupKeyActivity.FIRST_RUN_MODE);

            setupKeyActivityIntent.putExtra("password", password);
            activity.startActivity(setupKeyActivityIntent);
        }
    }

    public void proceedKeyRecovery() {
        if (setPassword()) {
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
        if (setPassword()) {
            try {
                App.getAppContainer().getCryptoManager().changePassword(password);
                activity.resetPassword(activity.getString(R.string.updated));

                activity.startActivity(new Intent(App.getContext(), MainActivity.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean setPassword() {
        TextView topLabel = activity.findViewById(R.id.auth_top_label);
        TextView censoredPasswordView = activity.findViewById(R.id.censored_password_text_view);

        String repeatCodeString = activity.getString(R.string.repeat_access_code);

        if (password != null && topLabel.getText().equals(repeatCodeString)) {
            if (!passwordBuilder.toString().equals(password)) {
                activity.resetPassword(activity.getString(R.string.code_not_match));
                return false;
            }

            censoredPasswordView.setText("");
            return true;
        } else {
            if (passwordBuilder.length() >= MIN_PASSWORD_LENGTH) {
                password = passwordBuilder.toString();
                passwordBuilder.setLength(0);

                topLabel.setText(repeatCodeString);
                censoredPasswordView.setText("");
            } else {
                String messageFormat = activity.getString(R.string.minimum_password_length_is_n);
                activity.resetPassword(String.format(messageFormat, MIN_PASSWORD_LENGTH));
            }
        }

        return false;
    }
}
