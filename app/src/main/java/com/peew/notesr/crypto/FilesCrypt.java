package com.peew.notesr.crypto;

import android.util.Base64;
import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.model.EncryptedFile;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.File;
import com.peew.notesr.model.FileInfo;

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

    public static EncryptedFileInfo encryptInfo(FileInfo fileInfo, CryptoKey cryptoKey) {
        File file = new File(fileInfo.getName(), fileInfo.getType(), null);

        file.setId(fileInfo.getId());
        file.setNoteId(fileInfo.getNoteId());

        EncryptedFile encryptedFile = encrypt(file, cryptoKey);

        return new EncryptedFileInfo(
                encryptedFile.getId(),
                encryptedFile.getNoteId(),
                encryptedFile.getEncryptedName(),
                encryptedFile.getEncryptedType()
        );
    }

    public static FileInfo decryptInfo(EncryptedFileInfo encryptedFileInfo, CryptoKey cryptoKey) {
        EncryptedFile encryptedFile = new EncryptedFile(
                encryptedFileInfo.getNoteId(),
                encryptedFileInfo.getEncryptedName(),
                encryptedFileInfo.getEncryptedType(),
                null
        );

        encryptedFile.setId(encryptedFileInfo.getId());
        File file = decrypt(encryptedFile, cryptoKey);

        return new FileInfo(
                file.getId(),
                file.getNoteId(),
                file.getName(),
                file.getType()
        );
    }

    public static EncryptedFile encrypt(File file, CryptoKey cryptoKey) {
        Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());

        try {
            byte[] nameBytes = aes.encrypt(file.getName().getBytes(StandardCharsets.UTF_8));
            byte[] data = null;

            if (file.getData() != null) {
                data = aes.encrypt(file.getData());
            }

            String name = Base64.encodeToString(nameBytes, Base64.DEFAULT);
            String type = null;

            if (file.getType() != null) {
                byte[] typeBytes = aes.encrypt(file.getType().getBytes(StandardCharsets.UTF_8));
                type = Base64.encodeToString(typeBytes, Base64.DEFAULT);
            }

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
            byte[] decryptedData = null;

            if (encryptedFile.getEncryptedData() != null) {
                decryptedData = aes.decrypt(encryptedFile.getEncryptedData());
            }

            String decryptedName = new String(aes.decrypt(decodedName));
            String decryptedType = null;

            if (encryptedFile.getEncryptedType() != null) {
                byte[] decodedType = Base64.decode(encryptedFile.getEncryptedType(), Base64.DEFAULT);
                decryptedType = new String(aes.decrypt(decodedType));
            }

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
