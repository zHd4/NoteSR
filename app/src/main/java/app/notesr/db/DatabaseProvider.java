package app.notesr.db;

import android.content.Context;

import androidx.room.Room;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import app.notesr.security.crypto.CryptoManager;

public class DatabaseProvider {
    private static final String DB_NAME = "notesr.db";
    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            SQLiteDatabase.loadLibs(context);

            CryptoManager cryptoManager = CryptoManager.getInstance();
            byte[] passphrase = cryptoManager.getSecrets().getKey();

            SupportFactory factory = new SupportFactory(passphrase);

            instance = Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                    .openHelperFactory(factory)
                    .build();
        }

        return instance;
    }
}

