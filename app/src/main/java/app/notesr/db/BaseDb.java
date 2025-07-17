package app.notesr.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import app.notesr.App;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseDb extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    protected final Map<Class<? extends BaseDao>, BaseDao> tables = new HashMap<>();

    public final SQLiteDatabase readableDatabase;
    public final SQLiteDatabase writableDatabase;

    public BaseDb(String name) {
        super(App.getContext(), name, null, DATABASE_VERSION);

        this.readableDatabase = getReadableDatabase();
        this.writableDatabase = getWritableDatabase();
    }

    public <T extends BaseDao> T getTable(Class<? extends BaseDao> tableClass) {
        return (T) tables.get(tableClass);
    }

    public void beginTransaction() {
        writableDatabase.beginTransaction();
    }

    public void rollbackTransaction() {
        writableDatabase.endTransaction();
    }

    public void commitTransaction() {
        writableDatabase.setTransactionSuccessful();
        writableDatabase.endTransaction();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
