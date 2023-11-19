package com.notesr.controllers.generators;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.notesr.controllers.ActivityHelper;
import com.notesr.models.Note;
import com.notesr.models.OpenNoteOperation;
import com.notesr.controllers.activities.NoteActivity;

public class TablesGenerator extends ActivityHelper {
    private int currentIndex = 0;

    private void fillTable(
            final Context context,
            final Activity activity,
            TableLayout table,
            final String[] rows,
            final int backgroundColor,
            final View.OnClickListener onClickListener
    ) {
        int beforeLineColor = Color.rgb(25, 28, 33);

        if (rows.length > 0) {
            for (int i = 0; i < rows.length; i++) {
                if(rows[i] == null) {
                    continue;
                }

                TableRow trData = new TableRow(context);

                trData.setBackgroundColor(backgroundColor);
                trData.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final TextView tableRow = new TextView(context);

                tableRow.setId(i);
                tableRow.setText(rows[i]);

                tableRow.setTextColor(Color.WHITE);
                tableRow.setTextSize(24);
                tableRow.setPadding(15, 35, 15, 35);
                trData.addView(tableRow);

                TableRow beforeLine = new TableRow(context);

                beforeLine.setBackgroundColor(beforeLineColor);
                beforeLine.setPadding(15, 2, 15, 2);

                table.addView(trData, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                table.addView(beforeLine, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                trData.setOnClickListener(onClickListener);
                this.currentIndex++;
            }
        }
    }

    public void fillNotesTable(
            final Context context,
            final Activity activity,
            TableLayout table,
            final Note[] notes,
            final int noteBackgroundColor
    ) {
        String[] rowsNames = new String[notes.length];
        View.OnClickListener listener = (v -> {
            String noteTitle = notes[currentIndex].getName();

            NoteActivity.setNoteId(currentIndex);
            NoteActivity.setNoteTitle(noteTitle);

            NoteActivity.operation = OpenNoteOperation.EDIT_NOTE;

            activity.startActivity(getIntent(
                    context,
                    NoteActivity.class
            ));
        });

        for(int i = 0; i < rowsNames.length; i++) {
            rowsNames[i] = notes[i].getName();
        }

        fillTable(context, activity, table, rowsNames, noteBackgroundColor, listener);
    }
}
