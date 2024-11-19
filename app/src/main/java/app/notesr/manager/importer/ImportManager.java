package app.notesr.manager.importer;

import android.content.Context;

import java.io.FileInputStream;

public class ImportManager extends BaseImportManager{
    public ImportManager(Context context, FileInputStream sourceStream) {
        super(context, sourceStream);
    }

    @Override
    public void start() {

    }
}
