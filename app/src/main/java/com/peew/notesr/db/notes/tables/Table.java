package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;
import android.util.Base64;
import android.util.Log;

import com.peew.notesr.crypto.Aes;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Table {
    protected SQLiteOpenHelper helper;
    public abstract String getName();
    public abstract Map<String, String> getFields();

    public Table(SQLiteOpenHelper helper) {
        this.helper = helper;
    }

    protected List<String> getFieldsNames() {
        return new ArrayList<>(getFields().keySet());
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
            return new String(getAesInstance().decrypt(decoded));
        } catch (Exception e) {
            Log.e("Table.decrypt error", e.toString());
            throw new RuntimeException(e);
        }
    }

    private Aes getAesInstance() {
        CryptoKey cryptoKey = CryptoManager.getInstance().getCryptoKeyInstance();
        return new Aes(cryptoKey.getKey(), cryptoKey.getSalt());
    }
}