package app.notesr.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;

import app.notesr.ActivityBase;

public final class ActivityUtils {
    public static void showToastMessage(ActivityBase activity, String text, int duration) {
        Toast toast = Toast.makeText(activity, text, duration);
        toast.show();
    }

    public static void copyToClipboard(ActivityBase activity, String text) {
        ClipboardManager clipboard =
                (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", text);

        clipboard.setPrimaryClip(clip);
    }

    public static void disableBackButton(ActivityBase activity) {
        OnBackPressedDispatcher dispatcher = activity.getOnBackPressedDispatcher();
        dispatcher.addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                activity.finishAffinity();
            }
        });
    }
}
