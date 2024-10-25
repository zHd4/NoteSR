package notesr.activity.files.viewer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import notesr.App;
import notesr.R;
import notesr.activity.ExtendedAppCompatActivity;
import notesr.activity.files.AssignmentsListActivity;
import notesr.manager.AssignmentsManager;
import notesr.model.FileInfo;
import notesr.tools.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        getMenuInflater().inflate(R.menu.menu_open_assignment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.saveImageButton) {
            saveFileOnClick();
        } else if (id == R.id.deleteImageButton) {
            deleteFileOnClick();
        }

        return super.onOptionsItemSelected(item);
    }

    private void executeTask(Runnable task, int dialogLayoutResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogLayoutResId)
                .setCancelable(false);

        AlertDialog progressDialog = builder.create();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            runOnUiThread(progressDialog::show);
            task.run();
            runOnUiThread(progressDialog::dismiss);
        });
    }

    private void returnToListActivity() {
        Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

        intent.putExtra("noteId", fileInfo.getNoteId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);
    }

    protected void loadFileInfo() {
        //noinspection deprecation
        fileInfo = (FileInfo) getIntent().getSerializableExtra("fileInfo");

        if (fileInfo == null) {
            throw new RuntimeException("File info not provided");
        }
    }

    protected void saveFileOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_are_you_sure)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.save, (dialog, result) -> saveFile())
                .setNegativeButton(R.string.no, (dialog, result) -> {});

        builder.create().show();
    }

    private void saveFile() {
        File destFile = Paths.get(saveDir.toPath().toString(), fileInfo.getName()).toFile();
        Runnable saveRunnable = getSaveRunnable(destFile);

        int dialogLayoutId = R.layout.progress_dialog_saving;

        if (destFile.exists()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setView(R.layout.dialog_file_already_exists)
                    .setTitle(R.string.warning)
                    .setPositiveButton(R.string.overwrite, (dialog, result) -> executeTask(saveRunnable, dialogLayoutId))
                    .setNegativeButton(R.string.no, (dialog, result) ->
                            showToastMessage(getResources().getString(R.string.saving_canceled), Toast.LENGTH_SHORT));

            builder.create().show();
        } else {
            executeTask(saveRunnable, dialogLayoutId);
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

                Looper.prepare();
                showToastMessage(String.format(messageFormat, destFile.getAbsolutePath()), Toast.LENGTH_LONG);
            } catch (RuntimeException e) {
                Log.e("NoteSR", e.toString());

                Looper.prepare();
                showToastMessage(getResources().getString(R.string.cannot_save_file), Toast.LENGTH_SHORT);
            }
        };
    }

    protected void deleteFileOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(R.layout.dialog_action_cannot_be_undo)
                .setTitle(R.string.warning)
                .setNegativeButton(R.string.no, (dialog, result) -> {})
                .setPositiveButton(R.string.delete, (dialog, result) -> executeTask(() -> {
                    deleteFile();
                    returnToListActivity();
                }, R.layout.progress_dialog_deleting));

        builder.create().show();
    }

    private void deleteFile() {
        App.getAppContainer()
                .getAssignmentsManager()
                .delete(fileInfo.getId());
    }
}
