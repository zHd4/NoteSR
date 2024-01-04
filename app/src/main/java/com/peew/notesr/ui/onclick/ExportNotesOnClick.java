package com.peew.notesr.ui.onclick;

import android.widget.Toast;

import com.peew.notesr.R;
import com.peew.notesr.db.notes.NotesExporter;
import com.peew.notesr.ui.MainActivity;

import java.util.function.Consumer;

public class ExportNotesOnClick implements Consumer<MainActivity> {
    @Override
    public void accept(MainActivity activity) {
        try {
            String path = new NotesExporter(activity).export();
            String message = String.format(activity.getString(R.string.saved_to), path);

            activity.showToastMessage(message, Toast.LENGTH_SHORT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
