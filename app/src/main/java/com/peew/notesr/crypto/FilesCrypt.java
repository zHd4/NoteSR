package com.peew.notesr.crypto;

import android.util.Base64;
import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.model.EncryptedFile;
import com.peew.notesr.model.File;

import java.nio.charset.StandardCharsets;

public class FilesCrypt {
    public static EncryptedFile encrypt(File file) {
        return encrypt(file, getCryptoManager().getCryptoKeyInstance());
    }

    public static File decrypt(EncryptedFile file) {
        return decrypt(file, getCryptoManager().getCryptoKeyInstance());
    }

    public static EncryptedFile encrypt(File file, CryptoKey cryptoKey) {
        Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());

        try {
            byte[] nameBytes = aes.encrypt(file.getName().getBytes(StandardCharsets.UTF_8));
            byte[] data = aes.encrypt(file.getData());

            String name = Base64.encodeToString(nameBytes, Base64.DEFAULT);

            EncryptedFile encryptedFile = new EncryptedFile(file.getNoteId(), name, data);

            encryptedFile.setId(file.getId());
            return encryptedFile;
        } catch (Exception e) {
            Log.e("Cannot encrypt file", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static File decrypt(EncryptedFile encryptedFile, CryptoKey cryptoKey) {
        Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());

        try {
            byte[] decodedName = Base64.decode(encryptedFile.getEncryptedName(), Base64.DEFAULT);
            String decryptedName = new String(aes.decrypt(decodedName));
            byte[] decryptedData = aes.encrypt(encryptedFile.getEncryptedData());

            File file = new File(decryptedName, decryptedData);

            file.setId(encryptedFile.getId());
            file.setNoteId(encryptedFile.getNoteId());

            return file;
        } catch (Exception e) {
            Log.e("Cannot encrypt file", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
