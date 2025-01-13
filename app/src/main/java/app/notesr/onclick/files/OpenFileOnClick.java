package app.notesr.onclick.files;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import app.notesr.App;
import app.notesr.activity.files.FilesListActivity;
import app.notesr.activity.files.viewer.FileViewerActivityBase;
import app.notesr.activity.files.viewer.OpenImageActivity;
import app.notesr.activity.files.viewer.OpenTextFileActivity;
import app.notesr.activity.files.viewer.OpenUnknownFileActivity;
import app.notesr.activity.files.viewer.OpenVideoActivity;
import app.notesr.model.FileInfo;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class OpenFileOnClick implements AdapterView.OnItemClickListener {
    private static final Map<String, Class<? extends FileViewerActivityBase>> FILES_VIEWERS =
            Map.of(
                    "text", OpenTextFileActivity.class,
                    "image", OpenImageActivity.class,
                    "video", OpenVideoActivity.class
//                    "audio", null
            );

    private final FilesListActivity activity;
    private final Map<Long, String> filesIdsMap;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String fileId = filesIdsMap.get(id);

        FileInfo fileInfo = App.getAppContainer()
                .getFilesService()
                .getInfo(fileId);

        String type = getFileType(fileInfo);

        Class<? extends FileViewerActivityBase> viewer = type != null
                ? FILES_VIEWERS.get(type)
                : OpenUnknownFileActivity.class;

        openViewer(viewer, fileInfo);
    }

    private void openViewer(Class<? extends FileViewerActivityBase> viewer, FileInfo fileInfo) {
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
