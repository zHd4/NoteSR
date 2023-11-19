package com.notesr.controllers.generators;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.notesr.controllers.ActivityHelper;
import com.notesr.models.Note;
import com.notesr.models.OpenNoteOperation;
import com.notesr.controllers.activities.NoteActivity;

public class TableGenerator extends ActivityHelper {
    public void fillTable(
            final Context context,
            final Activity activity,
            TableLayout table,
            final Note[] notes,
            final int noteColor,
            final int maxTitleVisualSize
    ) {
        int beforeLineColor = Color.rgb(25, 28, 33);

        if (notes.length > 0) {
            for (int i = 0; i < notes.length; i++) {
                if(notes[i] == null) {
                    continue;
                }

                String title = notes[i].getName();
                TableRow trData = new TableRow(context);

                trData.setBackgroundColor(noteColor);
                trData.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final TextView notesElement = new TextView(context);

                notesElement.setId(i);

                notesElement.setText(notes[i].getName().length() > maxTitleVisualSize
                        ? notes[i].getName().substring(0, maxTitleVisualSize) + "..."
                        : notes[i].getName());

                notesElement.setTextColor(Color.WHITE);
                notesElement.setTextSize(24);
                notesElement.setPadding(15, 35, 15, 35);
                trData.addView(notesElement);

                TableRow beforeLine = new TableRow(context);

                beforeLine.setBackgroundColor(beforeLineColor);
                beforeLine.setPadding(15, 2, 15, 2);

                table.addView(trData, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                table.addView(beforeLine, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final int finalIndex = i;

                trData.setOnClickListener(v -> {
                    int id = finalIndex;
                    String noteTitle = notes[id].getName();

                    NoteActivity.setNoteId(id);
                    NoteActivity.setNoteTitle(noteTitle);

                    NoteActivity.operation = OpenNoteOperation.EDIT_NOTE;

                    activity.startActivity(getIntent(
                            context,
                            NoteActivity.class
                    ));
                });
            }
        }
    }
}
