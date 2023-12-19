package com.peew.notesr.tools;

import android.app.Activity;
import android.content.DialogInterface;
import android.text.Html;
import android.text.Spanned;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.peew.notesr.App;
import com.peew.notesr.R;

public class AlertDialogHelper {
    public static AlertDialog generateYesNoDialog(Activity activity,
                                                  String messageText,
                                                  String positiveButtonText,
                                                  String negativeButtonText,
                                                  DialogInterface.OnClickListener onClickListener) {
        int dialogStyleId = R.style.AlertDialogTheme;
        int textColor = ContextCompat.getColor(App.getContext(), R.color.text_color);

        String dialogTextColor = toHexColor(textColor);
        String messageHtmlFormat = "<font color='%s'>%s</font>";

        String message = String.format(messageHtmlFormat, dialogTextColor, messageText);
        Spanned messageSpanned = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, dialogStyleId);
        builder.setMessage(messageSpanned)
                .setPositiveButton(positiveButtonText, onClickListener)
                .setNegativeButton(negativeButtonText, onClickListener);

        return builder.create();
    }

    private static String toHexColor(int color) {
        return "#" + Integer.toHexString(color).substring(2);
    }
}
