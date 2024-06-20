package com.peew.notesr.activity.files.viewer;

import android.util.Log;
import android.widget.Toast;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.tools.FileManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileViewerActivityBase extends AppCompatActivityExtended {
    protected File file;

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
            throw new RuntimeException(e);
        }
    }

    protected void saveFile(java.io.File dir) {
        java.io.File destFile = Paths.get(dir.toPath().toString(), file.getName()).toFile();

        try {
            FileManager.writeFileBytes(destFile, file.getData());

            String messageFormat = getResources().getString(R.string.saved_to);
            showToastMessage(String.format(messageFormat, destFile.getAbsolutePath()), Toast.LENGTH_LONG);
        } catch (IOException e) {
            Log.e("FileViewerActivityBase.saveFile", e.toString());
            showToastMessage(getResources().getString(R.string.cannot_save_file), Toast.LENGTH_SHORT);
        }
    }
}
