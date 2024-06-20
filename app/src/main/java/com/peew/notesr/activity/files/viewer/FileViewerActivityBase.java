package com.peew.notesr.activity.files.viewer;

import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.tools.FileManager;

import java.io.IOException;
import java.nio.file.Paths;

public class FileViewerActivityBase extends AppCompatActivityExtended {
    protected File file;
    protected java.io.File saveDir;

    protected void loadFile() {
        //noinspection deprecation
        FileInfo fileInfo = (FileInfo) getIntent().getSerializableExtra("file_info");

        if (fileInfo == null) {
            throw new RuntimeException("File info not provided");
        }

        try {
            AssignmentsManager manager = App.getAppContainer().getAssignmentsManager();
            byte[] fileData = FilesCrypt.decryptData(manager.get(fileInfo.getId()));

            file = new File(fileInfo);
            file.setData(fileData);
        } catch (IOException e) {
            Log.e("FileViewerActivityBase.loadFile", e.toString());
            throw new RuntimeException(e);
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
        java.io.File destFile = Paths.get(saveDir.toPath().toString(), file.getName()).toFile();

        Runnable save = () -> {
            try {
                FileManager.writeFileBytes(destFile, file.getData());

                String messageFormat = getResources().getString(R.string.saved_to);
                showToastMessage(String.format(messageFormat, destFile.getAbsolutePath()), Toast.LENGTH_LONG);
            } catch (IOException e) {
                Log.e("FileViewerActivityBase.saveFile", e.toString());
                showToastMessage(getResources().getString(R.string.cannot_save_file), Toast.LENGTH_SHORT);
            }
        };

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
}
