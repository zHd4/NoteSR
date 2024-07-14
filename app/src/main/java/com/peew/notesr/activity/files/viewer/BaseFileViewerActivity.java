package com.peew.notesr.activity.files.viewer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.activity.files.AssignmentsListActivity;
import com.peew.notesr.manager.AssignmentsManager;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.tools.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class BaseFileViewerActivity extends ExtendedAppCompatActivity {
    protected FileInfo fileInfo;
    protected java.io.File saveDir;
    protected static boolean running;

    public static boolean isRunning() {
        return running;
    }

    @Override
    protected void onStart() {
        super.onStart();
        running = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        running = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadFileInfo();

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(fileInfo.getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_open_media, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.save_image_to_storage_button) {
            saveFileOnClick();
        } else if (id == R.id.delete_image_button) {
            deleteFileOnClick();
        }

        return super.onOptionsItemSelected(item);
    }

    private void returnToListActivity() {
        Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

        intent.putExtra("note_id", fileInfo.getNoteId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }

    protected void loadFileInfo() {
        //noinspection deprecation
        fileInfo = (FileInfo) getIntent().getSerializableExtra("file_info");

        if (fileInfo == null) {
            throw new RuntimeException("File info not provided");
        }
    }

    protected void saveFileOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.save, (dialog, result) -> saveFile())
                .setNegativeButton(R.string.no, (dialog, result) -> {});

        builder.create().show();
    }

    private void saveFile() {
        File destFile = Paths.get(saveDir.toPath().toString(), fileInfo.getName()).toFile();
        Runnable save = getSaveRunnable(destFile);

        if (destFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setView(R.layout.dialog_file_already_exists)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.overwrite, (dialog, result) -> save.run())
                    .setNegativeButton(R.string.no, (dialog, result) ->
                            showToastMessage(getResources().getString(R.string.saving_canceled), Toast.LENGTH_SHORT));

            builder.create().show();
        } else {
            save.run();
        }
    }

    private Runnable getSaveRunnable(File destFile) {
        return () -> {
            AssignmentsManager assignmentsManager = App.getAppContainer().getAssignmentsManager();

            try {
                assignmentsManager.read(fileInfo.getId(), chunk -> {
                    try {
                        FileManager.writeFileBytes(destFile, chunk, true);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                String messageFormat = getResources().getString(R.string.saved_to);
                showToastMessage(String.format(messageFormat, destFile.getAbsolutePath()), Toast.LENGTH_LONG);
            } catch (RuntimeException e) {
                Log.e("NoteSR", e.toString());
                showToastMessage(getResources().getString(R.string.cannot_save_file), Toast.LENGTH_SHORT);
            }
        };
    }

    protected void deleteFileOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.delete, (dialog, result) -> {
                    deleteFile();
                    returnToListActivity();
                })
                .setNegativeButton(R.string.no, (dialog, result) -> {});

        builder.create().show();
    }

    private void deleteFile() {
        App.getAppContainer()
                .getAssignmentsManager()
                .delete(fileInfo.getId());
    }
}
