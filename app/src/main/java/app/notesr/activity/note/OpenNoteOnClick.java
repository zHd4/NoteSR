/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.note;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class OpenNoteOnClick implements AdapterView.OnItemClickListener {
    private final Context context;
    private final Map<Long, String> notesIdsMap;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String noteId = notesIdsMap.get(id);
        Intent noteOpenActivtyIntent = new Intent(context, OpenNoteActivity.class);

        noteOpenActivtyIntent.putExtra("noteId", noteId);
        context.startActivity(noteOpenActivtyIntent);
    }
}
