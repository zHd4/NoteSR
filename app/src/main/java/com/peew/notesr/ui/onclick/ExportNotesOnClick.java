package com.peew.notesr.ui.onclick;

import com.peew.notesr.db.notes.NotesExporter;
import com.peew.notesr.ui.MainActivity;

import java.util.function.Consumer;

public class ExportNotesOnClick implements Consumer<MainActivity> {
    @Override
    public void accept(MainActivity activity) {
        try {
            new NotesExporter(activity).export();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
