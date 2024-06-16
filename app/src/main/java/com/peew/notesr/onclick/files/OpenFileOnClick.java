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
import com.peew.notesr.model.EncryptedFileInfo;
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
        FileInfo fileInfo = FilesCrypt.decryptInfo(filesTable.get(id));

        String type = fileInfo.getType().split("/")[0];

        if (FILES_VIEWERS.containsKey(type)) {
            Class<? extends FileViewerActivityBase> viewer = FILES_VIEWERS.get(type);
            Intent intent = new Intent(App.getContext(), viewer);

            intent.putExtra("file_info", fileInfo);
            activity.startActivity(intent);
        }
    }
}
