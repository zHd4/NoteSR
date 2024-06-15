package com.peew.notesr.onclick.files;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import com.peew.notesr.App;
import com.peew.notesr.activity.files.AssignmentsListActivity;
import com.peew.notesr.activity.files.viewer.FileViewerActivityBase;
import com.peew.notesr.activity.files.viewer.OpenImageActivity;
import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;
import java.util.Map;

public class OpenFileOnClick implements AdapterView.OnItemClickListener {
    private static final Map<String, Class<? extends FileViewerActivityBase>> FILES_VIEWERS =
            Map.of(
//                    "text", null,
                    "image", OpenImageActivity.class
//                    "video", null,
//                    "audio", null
            );

    private final AssignmentsListActivity activity;

    public OpenFileOnClick(AssignmentsListActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
        AssignmentsManager manager = App.getAppContainer().getAssignmentsManager();

        File file = new File(FilesCrypt.decryptInfo(filesTable.get(id)));

        try {
            file.setData(manager.get(file.getId()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String type = file.getType().split("/")[0];

        if (FILES_VIEWERS.containsKey(type)) {
            Class<? extends FileViewerActivityBase> viewer = FILES_VIEWERS.get(type);
            Intent intent = new Intent(App.getContext(), viewer);

            intent.putExtra("file", file);
            activity.startActivity(intent);
        }
    }
}
