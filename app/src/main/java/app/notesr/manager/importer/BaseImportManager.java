package app.notesr.manager.importer;

import android.content.Context;

import java.io.FileInputStream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseImportManager {
    protected final Context context;
    protected final FileInputStream sourceStream;

    @Getter
    protected ImportResult result = ImportResult.NONE;

    @Getter
    protected String status = "";

    public abstract void start();
}
