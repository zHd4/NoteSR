package app.notesr.manager.data.importer;

import android.content.Context;

import java.io.File;
import java.time.format.DateTimeFormatter;

import app.notesr.App;
import app.notesr.manager.BaseManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseImportManager extends BaseManager {
    protected final Context context;
    protected final File file;

    private boolean transactionStarted = false;

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }

    protected boolean isTransactionStarted() {
        return transactionStarted;
    }

    protected void begin() {
        App.getAppContainer().getNotesDB().beginTransaction();
        transactionStarted = true;
    }

    protected void rollback() {
        App.getAppContainer().getNotesDB().rollbackTransaction();
        transactionStarted = false;
    }

    protected void end() {
        App.getAppContainer().getNotesDB().commitTransaction();
        transactionStarted = false;
    }

    public abstract void start();
    public abstract ImportResult getResult();
    public abstract String getStatus();
}
