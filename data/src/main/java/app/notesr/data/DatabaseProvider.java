package app.notesr.data;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.data.migration.MigrationRegistry;

public final class DatabaseProvider {
    public static final String DB_NAME = "notesr.db";
    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        byte[] passphrase = cryptoManager.getSecrets().getKey();

        SupportFactory factory = new SupportFactory(passphrase);
        return getInstance(context, factory);
    }

    public static AppDatabase getInstance(Context context, SupportFactory factory) {
        synchronized (DatabaseProvider.class) {
            if (instance == null) {
                SQLiteDatabase.loadLibs(context);

                instance = Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                        .openHelperFactory(factory)
                        .addMigrations(MigrationRegistry.getAllMigrations()
                                .toArray(new Migration[0]))
                        .build();
            }
        }

        return instance;
    }

    public static void reinit(Context context, SupportFactory factory) {
        synchronized (DatabaseProvider.class) {
            if (instance != null) {
                instance.close();
                instance = null;
            }

            getInstance(context, factory);
        }
    }

    public static void close() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}

