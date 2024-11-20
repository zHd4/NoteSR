package app.notesr.manager.importer;

import android.content.Context;

import java.io.File;

import app.notesr.manager.BaseManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseImportManager extends BaseManager {
    protected final Context context;
    protected final File file;

    protected void clearTables() {
        getDataBlocksTable().deleteAll();
        getFilesInfoTable().deleteAll();
        getNotesTable().deleteAll();
    }

    public abstract void start();
    public abstract ImportResult getResult();
    public abstract String getStatus();
}
