package com.peew.notesr.crypto;

import android.util.Base64;
import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.model.EncryptedFile;
import com.peew.notesr.model.File;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class FilesCrypt {
    public static List<File> decrypt(List<EncryptedFile> files) {
        return decrypt(files, getCryptoManager().getCryptoKeyInstance());
    }

    public static List<File> decrypt(List<EncryptedFile> files, CryptoKey cryptoKey) {
        return files.stream()
                .map(file -> decrypt(file, cryptoKey))
                .collect(Collectors.toList());
    }

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
            byte[] typeBytes = aes.encrypt(file.getType().getBytes(StandardCharsets.UTF_8));

            byte[] data = aes.encrypt(file.getData());

            String name = Base64.encodeToString(nameBytes, Base64.DEFAULT);
            String type = Base64.encodeToString(typeBytes, Base64.DEFAULT);

            EncryptedFile encryptedFile = new EncryptedFile(file.getNoteId(), name, type, data);

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
            byte[] decodedType = Base64.decode(encryptedFile.getEncryptedType(), Base64.DEFAULT);

            String decryptedName = new String(aes.decrypt(decodedName));
            String decryptedType = new String(aes.decrypt(decodedType));

            byte[] decryptedData = aes.decrypt(encryptedFile.getEncryptedData());

            File file = new File(decryptedName, decryptedType, decryptedData);

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
