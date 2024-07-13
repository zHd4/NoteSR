package com.peew.notesr.onclick.files;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import com.peew.notesr.App;
import com.peew.notesr.activity.files.AssignmentsListActivity;
import com.peew.notesr.activity.files.viewer.FileViewerActivityBase;
import com.peew.notesr.activity.files.viewer.OpenImageActivity;
import com.peew.notesr.activity.files.viewer.OpenTextFileActivity;
import com.peew.notesr.activity.files.viewer.OpenVideoActivity;
import com.peew.notesr.model.FileInfo;

import java.util.Map;

public class OpenFileOnClick implements AdapterView.OnItemClickListener {
    private static final Map<String, Class<? extends FileViewerActivityBase>> FILES_VIEWERS =
            Map.of(
                    "text", OpenTextFileActivity.class,
                    "image", OpenImageActivity.class,
                    "video", OpenVideoActivity.class
//                    "audio", null
            );

    private final AssignmentsListActivity activity;

    public OpenFileOnClick(AssignmentsListActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo fileInfo = App.getAppContainer()
                .getAssignmentsManager()
                .getInfo(id);

        String type = getFileType(fileInfo);

        if (type != null) {
            Class<? extends FileViewerActivityBase> viewer = FILES_VIEWERS.get(type);
            Intent intent = new Intent(App.getContext(), viewer);

            intent.putExtra("file_info", fileInfo);

            activity.startActivity(intent);
        }
    }

    private String getFileType(FileInfo fileInfo) {
        if (fileInfo.getType() != null) {
            String type = fileInfo.getType().split("/")[0];

            if (FILES_VIEWERS.containsKey(type)) {
                return type;
            }
        }

        return null;
    }
}
