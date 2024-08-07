package com.peew.notesr.onclick.files;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import com.peew.notesr.App;
import com.peew.notesr.activity.files.AssignmentsListActivity;
import com.peew.notesr.activity.files.viewer.BaseFileViewerActivity;
import com.peew.notesr.activity.files.viewer.OpenImageActivity;
import com.peew.notesr.activity.files.viewer.OpenTextFileActivity;
import com.peew.notesr.activity.files.viewer.OpenUnknownFileActivity;
import com.peew.notesr.activity.files.viewer.OpenVideoActivity;
import com.peew.notesr.model.FileInfo;

import java.util.Map;

public class OpenFileOnClick implements AdapterView.OnItemClickListener {
    private static final Map<String, Class<? extends BaseFileViewerActivity>> FILES_VIEWERS =
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

        Class<? extends BaseFileViewerActivity> viewer = type != null
                ? FILES_VIEWERS.get(type)
                : OpenUnknownFileActivity.class;

        openViewer(viewer, fileInfo);
    }

    private void openViewer(Class<? extends BaseFileViewerActivity> viewer, FileInfo fileInfo) {
        Intent intent = new Intent(App.getContext(), viewer);

        intent.putExtra("fileInfo", fileInfo);
        activity.startActivity(intent);
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
