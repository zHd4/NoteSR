package app.notesr.manager.importer;

import android.content.Context;

import java.io.FileInputStream;

import app.notesr.manager.BaseManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseImportManager extends BaseManager {
    protected final Context context;
    protected final FileInputStream sourceStream;

    public abstract void start();
    public abstract ImportResult getResult();
    public abstract String getStatus();
}
