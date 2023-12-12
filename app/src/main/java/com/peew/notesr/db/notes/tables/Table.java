package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public abstract class Table {
    private final CryptoKey cryptoKey;
    protected SQLiteOpenHelper helper;
    public abstract String getName();
    public abstract Map<String, String> getFields();

    public Table(SQLiteOpenHelper helper) {
        this.helper = helper;
        this.cryptoKey = CryptoManager.getInstance().getCryptoKeyInstance();
    }

    protected String encrypt(String text) {
        try {
            byte[] encrypted = getAesInstance().encrypt(text.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("Table.encrypt error", e.toString());
            throw new RuntimeException(e);
        }
    }

    protected String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.decode(encryptedText, Base64.DEFAULT);
            return Arrays.toString(getAesInstance().decrypt(decoded));
        } catch (Exception e) {
            Log.e("Table.decrypt error", e.toString());
            throw new RuntimeException(e);
        }
    }

    private Aes getAesInstance() {
        return new Aes(cryptoKey.getKey(), cryptoKey.getSalt());
    }
}
