package app.notesr.data.migration.changes;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class DropTempFilesTableMigration extends Migration {
    public DropTempFilesTableMigration(int startVersion, int endVersion) {
        super(startVersion, endVersion);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS temp_files");
    }
}
