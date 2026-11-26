/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import javax.inject.Inject;

import app.notesr.service.security.AppSecurityService;
import app.notesr.util.ActivityUtils;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Base activity for screens that use the app's shared window and security behavior.
 *
 * <p>Disables decor fitting for system windows and protects the window from screenshots.
 * Subclasses can apply system-bar insets and validate that authentication is configured.
 */
@AndroidEntryPoint
public class ActivityBase extends AppCompatActivity {

    @Inject
    AppSecurityService appSecurityService;

    @Inject
    ActivityUtils activityUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        enableWindowProtection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Applies the system-bar insets as padding to a view.
     *
     * @param main view that should receive system-bar padding
     */
    protected void applyInsets(View main) {
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /**
     * Checks whether app user is authenticated.
     *
     * @return {@code true} if authentication is configured
     */
    protected boolean isSessionActive() {
        return appSecurityService.isAuthConfigured();
    }

    /**
     * Restarts the app user is not authenticated
     */
    protected void validateSession() {
        if (!isSessionActive()) {
            restartApp();
        }
    }

    /**
     * Logs an activity error, displays the corresponding message, and finishes.
     *
     * @param tag        tag to use for the error log
     * @param messageId  string resource for the user-facing message
     * @param logMessage message to write to the error log
     * @param throwable  cause of the error
     */
    protected void finishWithError(
            String tag,
            int messageId,
            String logMessage,
            Throwable throwable) {
        Log.e(tag, logMessage, throwable);
        activityUtils.showToastMessage(getString(messageId), Toast.LENGTH_SHORT);
        finish();
    }

    /**
     * Starts the main activity with a cleared task stack, then finishes this activity.
     */
    void restartApp() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(mainActivityIntent);
        finish();
    }

    /**
     * Prevents this activity's window contents from appearing in screenshots or
     * non-secure displays.
     */
    void enableWindowProtection() {
        int windowFlag = WindowManager.LayoutParams.FLAG_SECURE;
        getWindow().setFlags(windowFlag, windowFlag);
    }
}
