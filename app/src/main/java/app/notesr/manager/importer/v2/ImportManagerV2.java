package app.notesr.manager.importer.v2;

import android.content.Context;

import java.io.File;

import app.notesr.manager.importer.BaseImportManager;
import app.notesr.manager.importer.ImportResult;
import lombok.Getter;

public class ImportManagerV2 extends BaseImportManager {
    @Getter
    private ImportResult result = ImportResult.NONE;

    @Getter
    private String status = "";

    public ImportManagerV2(Context context, File file) {
        super(context, file);
    }

    @Override
    public void start() {

    }
}
