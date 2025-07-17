package app.notesr.service.data.importer;

import android.content.Context;

import java.io.File;
import java.time.format.DateTimeFormatter;

import app.notesr.App;
import app.notesr.db.notes.NotesDb;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class ImportServiceBase {
    protected final Context context;
    protected final NotesDb notesDb;
    protected final File file;

    private boolean transactionStarted = false;

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }

    protected boolean isTransactionStarted() {
        return transactionStarted;
    }

    protected void begin() {
        notesDb.beginTransaction();
        transactionStarted = true;
    }

    protected void rollback() {
        notesDb.rollbackTransaction();
        transactionStarted = false;
    }

    protected void end() {
        notesDb.commitTransaction();
        transactionStarted = false;
    }

    public abstract void start();
    public abstract ImportResult getResult();
    public abstract String getStatus();
}
