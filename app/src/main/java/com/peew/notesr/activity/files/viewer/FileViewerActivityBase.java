package com.peew.notesr.activity.files.viewer;

import com.peew.notesr.App;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.component.AssignmentsManager;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;

import java.io.IOException;

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
}
