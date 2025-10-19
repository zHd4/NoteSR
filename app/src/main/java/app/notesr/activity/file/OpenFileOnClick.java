package app.notesr.activity.file;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import app.notesr.activity.file.viewer.FileViewerActivityBase;
import app.notesr.activity.file.viewer.OpenImageActivity;
import app.notesr.activity.file.viewer.OpenTextFileActivity;
import app.notesr.activity.file.viewer.OpenUnknownFileActivity;
import app.notesr.activity.file.viewer.OpenVideoActivity;
import app.notesr.data.model.FileInfo;
import app.notesr.service.file.FileService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public final class OpenFileOnClick implements AdapterView.OnItemClickListener {
    private static final Map<String, Class<? extends FileViewerActivityBase>> FILE_VIEWERS =
            Map.of(
                    "text", OpenTextFileActivity.class,
                    "image", OpenImageActivity.class,
                    "video", OpenVideoActivity.class
            );

    private final FilesListActivity activity;
    private final FileService fileService;
    private final Map<Long, String> filesIdsMap;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String fileId = filesIdsMap.get(id);

        newSingleThreadExecutor().execute(() -> {
            FileInfo fileInfo = fileService.getFileInfo(fileId);

            activity.runOnUiThread(() -> {
                String type = getFileType(fileInfo);

                Class<? extends FileViewerActivityBase> viewer = type != null
                        ? FILE_VIEWERS.get(type)
                        : OpenUnknownFileActivity.class;

                openViewer(viewer, fileInfo);
            });
        });
    }

    private void openViewer(Class<? extends FileViewerActivityBase> viewer, FileInfo fileInfo) {
        Intent intent = new Intent(activity.getApplicationContext(), viewer);

        intent.putExtra("fileInfo", fileInfo);
        activity.startActivity(intent);
    }

    private String getFileType(FileInfo fileInfo) {
        if (fileInfo.getType() != null) {
            String type = fileInfo.getType().split("/")[0];

            if (FILE_VIEWERS.containsKey(type)) {
                return type;
            }
        }

        return null;
    }
}
