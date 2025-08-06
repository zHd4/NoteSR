package app.notesr.file;

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
import app.notesr.ActivityBase;
import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.service.file.FileService;
import app.notesr.model.FileInfo;
import app.notesr.util.FileExifDataResolver;
import app.notesr.util.Wiper;
import app.notesr.util.thumbnail.ImageThumbnailCreator;
import app.notesr.util.thumbnail.ThumbnailCreator;
import app.notesr.util.thumbnail.VideoThumbnailCreator;

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

public class AddFileActivity extends ActivityBase {
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

        Map<FileInfo, File> filesMap = cacheFiles(getFilesUri(data));

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);

            AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
            FileService fileService = new FileService(db);

            filesMap.forEach((info, file) -> {
                try {
                    fileService.save(info, file);
                    Wiper.wipeFile(file);
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

    private Map<FileInfo, File> cacheFiles(List<Uri> uris) {
        Map<FileInfo, File> filesMap = new LinkedHashMap<>();

        uris.forEach(uri -> {

            try {
                FileInfo fileInfo = getFileInfo(uri);

                MimeType mimeType = fileInfo.getType() != null
                        ? MimeType.fromString(fileInfo.getType())
                        : null;

                File file = createTempFileFromUri(uri, mimeType);

                if (mimeType != null) {
                    fileInfo.setThumbnail(getFileThumbnail(file, mimeType));
                }

                filesMap.put(fileInfo, file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return filesMap;
    }

    private FileInfo getFileInfo(Uri uri) {
        FileExifDataResolver resolver = new FileExifDataResolver(uri);

        String filename = resolver.getFileName();
        String type = resolver.getMimeType();

        long size = resolver.getFileSize();

        FileInfo fileInfo = new FileInfo();

        fileInfo.setNoteId(noteId);
        fileInfo.setSize(size);
        fileInfo.setName(filename);
        fileInfo.setType(type);

        return fileInfo;
    }

    private InputStream getFileStream(Uri uri) {
        try {
            return getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getFileThumbnail(File file, MimeType mimeType) {
        try {
            String type = mimeType.type();
            ThumbnailCreator creator;

            if (type.equals("image")) {
                creator = new ImageThumbnailCreator();
            } else if (type.equals("video")) {
                creator = new VideoThumbnailCreator();
            } else {
                return null;
            }

            return requireNonNull(creator).getThumbnail(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createTempFileFromUri(Uri uri, MimeType mimeType) throws IOException {
        String extension = mimeType != null ? "." + mimeType.extension() : "";
        String fileName = randomUUID().toString() + extension;

        File file = new File(getCacheDir(), fileName);

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

    private record MimeType(String type, String extension) {
        public static MimeType fromString(String s) {
            String[] mimeTypeParts = s.split("/");
            return new MimeType(mimeTypeParts[0], mimeTypeParts[1]);
        }
    }
}