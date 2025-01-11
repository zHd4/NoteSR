package app.notesr.activity.files;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.service.FilesService;
import app.notesr.model.FileInfo;
import app.notesr.utils.FileExifDataResolver;
import app.notesr.utils.Wiper;
import app.notesr.utils.thumbnail.ImageThumbnailCreator;
import app.notesr.utils.thumbnail.ThumbnailCreator;
import app.notesr.utils.thumbnail.VideoThumbnailCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddFilesActivity extends ExtendedAppCompatActivity {
    private String noteId;
    private boolean noteModified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_file);

        noteId = getIntent().getStringExtra("noteId");

        if (noteId == null) {
            throw new RuntimeException("Note id didn't provided");
        }

        ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                addFilesCallback());

        Intent intent = new Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        resultLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_files)));
    }

    @Override
    public void finish() {
        Intent intent = new Intent(App.getContext(), FilesListActivity.class);

        intent.putExtra("noteId", noteId);
        intent.putExtra("modified", noteModified);
        startActivity(intent);

        super.finish();
    }

    private ActivityResultCallback<ActivityResult> addFilesCallback() {
        return result -> {
            int resultCode = result.getResultCode();

            if (resultCode == Activity.RESULT_OK) {
                if (result.getData() != null) {
                    addFiles(result.getData());
                } else {
                    throw new RuntimeException("Activity result is 'OK', but data not provided");
                }
            } else {
                finish();
            }
        };
    }

    private void addFiles(Intent data) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AlertDialog progressDialog = createProgressDialog();

        Map<FileInfo, InputStream> filesMap = getFilesMap(getFilesUri(data));

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);

            FilesService filesService = App.getAppContainer().getFilesService();

            filesMap.forEach((info, stream) -> {
                String fileId = filesService.saveInfo(info);

                try {
                    filesService.saveData(fileId, stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            runOnUiThread(() -> {
                progressDialog.dismiss();
                onFilesAddedCallback();
            });
        });
    }

    private void onFilesAddedCallback() {
        noteModified = true;
        finish();
    }

    private List<Uri> getFilesUri(Intent data) {
        List<Uri> result = new ArrayList<>();

        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            int filesCount = clipData.getItemCount();

            for (int i = 0; i < filesCount; i++) {
                result.add(clipData.getItemAt(i).getUri());
            }
        } else {
            result.add(data.getData());
        }

        return result;
    }

    private Map<FileInfo, InputStream> getFilesMap(List<Uri> uris) {
        Map<FileInfo, InputStream> map = new LinkedHashMap<>();

        uris.forEach(uri -> {
            FileInfo fileInfo = getFileInfo(uri);
            InputStream stream = getFileStream(uri);

            map.put(fileInfo, stream);
        });

        return map;
    }

    private FileInfo getFileInfo(Uri uri) {
        FileExifDataResolver resolver = new FileExifDataResolver(uri);

        String filename = resolver.getFileName();
        String type = resolver.getMimeType();

        byte[] thumbnail = getFileThumbnail(uri, type);
        long size = resolver.getFileSize();

        return FileInfo.builder()
                .noteId(noteId)
                .size(size)
                .name(filename)
                .type(type)
                .thumbnail(thumbnail)
                .build();
    }

    private InputStream getFileStream(Uri uri) {
        try {
            return getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getFileThumbnail(Uri uri, String mimeType) {
        try {
            String[] mimeTypeParts = mimeType.split("/");

            String type = mimeTypeParts[0];
            String extension = mimeTypeParts[1];

            File file = cloneFileFromUri(uri, extension);

            ThumbnailCreator creator;

            if (type.equals("image")) {
                creator = new ImageThumbnailCreator();
            } else if (type.equals("video")) {
                creator = new VideoThumbnailCreator(getApplicationContext());
            } else {
                return null;
            }

            byte[] thumbnail = requireNonNull(creator).getThumbnail(file);
            Wiper.wipeFile(file);

            return thumbnail;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File cloneFileFromUri(Uri uri, String extension) throws IOException {
        File file = new File(getCacheDir(), randomUUID().toString() + "." + extension);

        try (InputStream inputStream = getFileStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return file;
    }

    private AlertDialog createProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_adding).setCancelable(false);

        return builder.create();
    }
}