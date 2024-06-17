package com.peew.notesr.crypto;

import android.util.Base64;
import android.util.Log;
import com.peew.notesr.App;
import com.peew.notesr.model.EncryptedFile;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.FileInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class FilesCrypt {
    public static EncryptedFile updateKey(EncryptedFile file, CryptoKey oldKey, CryptoKey newKey) {
        EncryptedFileInfo fileInfo = new EncryptedFileInfo(file);
        byte[] data = file.getEncryptedData();

        EncryptedFileInfo reEncryptedFileInfo = encryptInfo(decryptInfo(fileInfo, oldKey), newKey);
        byte[] reEncryptedData = encryptData(decryptData(data, oldKey), newKey);

        EncryptedFile reEncryptedFile = new EncryptedFile(reEncryptedFileInfo);

        reEncryptedFile.setEncryptedData(reEncryptedData);
        reEncryptedFile.setSize((long) reEncryptedData.length);

        return reEncryptedFile;
    }

    public static List<FileInfo> decryptInfo(List<EncryptedFileInfo> filesInfo) {
        return decryptInfo(filesInfo, getCryptoManager().getCryptoKeyInstance());
    }

    public static List<FileInfo> decryptInfo(
            List<EncryptedFileInfo> filesInfo,
            CryptoKey cryptoKey) {
        return filesInfo.stream()
                .map(fileInfo -> decryptInfo(fileInfo, cryptoKey))
                .collect(Collectors.toList());
    }

    public static byte[] encryptData(byte[] data) {
        return encryptData(data, getCryptoManager().getCryptoKeyInstance());
    }

    public static byte[] decryptData(byte[] data) {
        return decryptData(data, getCryptoManager().getCryptoKeyInstance());
    }

    public static EncryptedFileInfo encryptInfo(FileInfo fileInfo) {
        return encryptInfo(fileInfo, getCryptoManager().getCryptoKeyInstance());
    }

    public static FileInfo decryptInfo(EncryptedFileInfo encryptedFileInfo) {
        return decryptInfo(encryptedFileInfo, getCryptoManager().getCryptoKeyInstance());
    }

    public static byte[] encryptData(byte[] data, CryptoKey cryptoKey) {
        try {
            Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());
            return aes.encrypt(data);
        } catch (Exception e) {
            Log.e("Cannot encrypt file data", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptData(byte[] data, CryptoKey cryptoKey) {
        try {
            Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());
            return aes.decrypt(data);
        } catch (Exception e) {
            Log.e("Cannot decrypt file data", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static EncryptedFileInfo encryptInfo(FileInfo fileInfo, CryptoKey cryptoKey) {
        Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());

        try {
            byte[] nameBytes = aes.encrypt(fileInfo.getName().getBytes(StandardCharsets.UTF_8));
            String name = Base64.encodeToString(nameBytes, Base64.DEFAULT);

            String type = null;

            if (fileInfo.getType() != null) {
                byte[] typeBytes = aes.encrypt(fileInfo.getType().getBytes(StandardCharsets.UTF_8));
                type = Base64.encodeToString(typeBytes, Base64.DEFAULT);
            }

            return new EncryptedFileInfo(
                    fileInfo.getId(),
                    fileInfo.getNoteId(),
                    fileInfo.getSize(),
                    name,
                    type,
                    fileInfo.getCreatedAt(),
                    fileInfo.getUpdatedAt());
        } catch (Exception e) {
            Log.e("Cannot encrypt file info", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static FileInfo decryptInfo(EncryptedFileInfo encryptedFileInfo, CryptoKey cryptoKey) {
        Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());

        try {
            byte[] decodedName = Base64.decode(encryptedFileInfo.getEncryptedName(), Base64.DEFAULT);
            String decryptedName = new String(aes.decrypt(decodedName));

            String decryptedType = null;

            if (encryptedFileInfo.getEncryptedType() != null) {
                byte[] decodedType = Base64.decode(encryptedFileInfo.getEncryptedType(), Base64.DEFAULT);
                decryptedType = new String(aes.decrypt(decodedType));
            }

            return new FileInfo(
                    encryptedFileInfo.getId(),
                    encryptedFileInfo.getNoteId(),
                    encryptedFileInfo.getSize(),
                    decryptedName,
                    decryptedType,
                    encryptedFileInfo.getCreatedAt(),
                    encryptedFileInfo.getUpdatedAt());
        } catch (Exception e) {
            Log.e("Cannot decrypt file info", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
