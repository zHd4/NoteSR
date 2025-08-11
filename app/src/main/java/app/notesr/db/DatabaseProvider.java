package app.notesr.db;

import android.content.Context;

import androidx.room.Room;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import app.notesr.security.crypto.CryptoManager;

public class DatabaseProvider {
    public static final String DB_NAME = "notesr.db";
    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        CryptoManager cryptoManager = CryptoManager.getInstance();
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

