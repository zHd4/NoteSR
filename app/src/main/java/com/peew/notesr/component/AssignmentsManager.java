package com.peew.notesr.component;

import com.peew.notesr.tools.FileManager;

import java.io.File;
import java.io.IOException;

public class AssignmentsManager {
    private static final String DIR_NAME = "assignments";

    public AssignmentsManager() {
        File dir = FileManager.getInternalFile(DIR_NAME);

        if (!dir.mkdir() && !dir.isDirectory()) {
            throw new RuntimeException("Cannot create directory " + dir.getAbsolutePath());
        }
    }

    public void save(Long id, byte[] data) throws IOException {
        FileManager.writeFileBytes(getInternalFile(id), data);
    }

    public byte[] get(Long id) throws IOException {
        return FileManager.readFileBytes(getInternalFile(id));
    }

    public void delete(Long id) {
        File file = getInternalFile(id);

        if (!file.delete()) {
            throw new RuntimeException("Cannot delete file " + file.getAbsolutePath());
        }
    }

    private File getInternalFile(Long id) {
        return getInternalFile(String.valueOf(id));
    }

    private File getInternalFile(String name) {
        return FileManager.getInternalFile(DIR_NAME + "/" + name);
    }
}
