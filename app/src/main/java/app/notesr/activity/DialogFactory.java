package app.notesr.activity;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;

import app.notesr.R;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DialogFactory {
    private final Activity activity;

    public AlertDialog buildThemedDialog(@LayoutRes int layoutRes) {
        return new AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                .setView(layoutRes)
                .create();
    }

    public AlertDialog.Builder themedBuilder(@LayoutRes int layoutRes) {
        return new AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                .setView(layoutRes);
    }

    public void showConfirmationDialog(@LayoutRes int layout,
                                       int titleRes,
                                       int confirmRes,
                                       DialogInterface.OnClickListener onConfirm) {
        themedBuilder(layout)
                .setTitle(titleRes)
                .setPositiveButton(confirmRes, onConfirm)
                .create()
                .show();
    }

    public void showOverwriteDialog(Runnable onOverwrite) {
        themedBuilder(R.layout.dialog_file_already_exists)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.overwrite, (d, w) -> onOverwrite.run())
                .create()
                .show();
    }
}
