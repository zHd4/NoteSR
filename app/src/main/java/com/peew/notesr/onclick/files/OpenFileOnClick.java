package com.peew.notesr.onclick.files;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.peew.notesr.App;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.activity.files.AssignmentsListActivity;
import com.peew.notesr.activity.files.OpenImageActivity;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.File;

import java.util.Map;

public class OpenFileOnClick implements AdapterView.OnItemClickListener {
    private static final Map<String, Class<? extends AppCompatActivityExtended>> FILES_VIEWERS =
            Map.of(
                    "text", null,
                    "image", OpenImageActivity.class,
                    "video", null,
                    "audio", null
            );

    private final AssignmentsListActivity activity;

    public OpenFileOnClick(AssignmentsListActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();

        File file = FilesCrypt.decrypt(filesTable.get(id));
        String type = file.getType().split("/")[0];

        if (FILES_VIEWERS.containsKey(type)) {
            Class<? extends AppCompatActivityExtended> viewer = FILES_VIEWERS.get(type);
            Intent intent = new Intent(App.getContext(), viewer);

            intent.putExtra("file", file);
            activity.startActivity(intent);
        }
    }
}
