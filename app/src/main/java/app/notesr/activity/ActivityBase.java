/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity;

import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import app.notesr.service.security.AppSecurityService;
import lombok.AccessLevel;
import lombok.Setter;

public class ActivityBase extends AppCompatActivity {

    @Setter(AccessLevel.MODULE) // For testing purposes only
    private AppSecurityService appSecurityService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        appSecurityService = new AppSecurityService(getApplicationContext());
        enableWindowProtection();
        validateSession();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void applyInsets(View main) {
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    protected boolean requiresSession() {
        return true;
    }

    protected boolean isSessionActive() {
        return appSecurityService.isAuthConfigured();
    }

    void restartApp() {
        Intent mainActivityIntent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(mainActivityIntent);
        finish();
    }

    void validateSession() {
        if (requiresSession() && !isSessionActive()) {
            restartApp();
        }
    }

    void enableWindowProtection() {
        int windowFlag = WindowManager.LayoutParams.FLAG_SECURE;
        getWindow().setFlags(windowFlag, windowFlag);
    }
}
