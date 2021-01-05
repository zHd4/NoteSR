package com.notesr.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.notesr.models.ActivityTools;

public class TableGenerator {
    public void fillTable(
            final Context context,
            final Activity activity,
            TableLayout table,
            final String[][] notes,
            final int noteColor,
            final int maxTitleVisualSize
    ) {
        int beforeLineColor = Color.rgb(25, 28, 33);

        if (!notes.equals(new String[0][0])) {
            for (int i = 0; i < notes.length; i++) {
                String title = notes[i][0];
                TableRow trData = new TableRow(context);

                trData.setBackgroundColor(noteColor);
                trData.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final TextView notesElement = new TextView(context);

                notesElement.setId(i);

                notesElement.setText(notes[i][0].length() > maxTitleVisualSize
                        ? notes[i][0].substring(0, maxTitleVisualSize) + "..."
                        : notes[i][0]);

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

                trData.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int id = finalIndex;
                        String noteTitle = notes[id][0];

                        NoteActivity.noteId = id;
                        NoteActivity.noteTitle = noteTitle;

                        NoteActivity.arg = NoteActivity.EDIT_NOTE;

                        activity.startActivity(ActivityTools.getIntent(
                                context,
                                NoteActivity.class
                        ));
                    }
                });
            }
        }
    }
}
