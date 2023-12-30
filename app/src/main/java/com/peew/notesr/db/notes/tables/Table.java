package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class Table {
    protected SQLiteOpenHelper helper;
    public abstract String getName();
    public abstract Map<String, String> getFields();
    public abstract void reEncryptAll(CryptoKey oldCryptoKey);

    public Table(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    protected String encrypt(String text) {
        return encrypt(text, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    protected String decrypt(String text) {
        return decrypt(text, CryptoManager.getInstance().getCryptoKeyInstance());
    }

    protected String encrypt(String text, CryptoKey cryptoKey) {
        try {
            Aes aesInstance = getAesInstance(cryptoKey);
            byte[] encrypted = aesInstance.encrypt(text.getBytes(StandardCharsets.UTF_8));

            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("Table.encrypt error", e.toString());
            throw new RuntimeException(e);
        }
    }

    protected String decrypt(String encryptedText, CryptoKey cryptoKey) {
        try {
            Aes aesInstance = getAesInstance(cryptoKey);
            byte[] decoded = Base64.decode(encryptedText, Base64.DEFAULT);

            return new String(aesInstance.decrypt(decoded));
        } catch (Exception e) {
            Log.e("Table.decrypt error", e.toString());
            throw new RuntimeException(e);
        }
    }

    private Aes getAesInstance(CryptoKey cryptoKey) {
        return new Aes(cryptoKey.key(), cryptoKey.salt());
    }
}
