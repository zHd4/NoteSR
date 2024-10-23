package com.peew.notesr.crypto;

import android.util.Log;
import com.peew.notesr.App;
import com.peew.notesr.model.EncryptedFileInfo;
import com.peew.notesr.model.FileInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class FilesCrypt {
    public static EncryptedFileInfo updateKey(EncryptedFileInfo fileInfo, CryptoKey oldKey, CryptoKey newKey) {
        return encryptInfo(decryptInfo(fileInfo, oldKey), newKey);
    }

    public static byte[] updateKey(byte[] data, CryptoKey oldKey, CryptoKey newKey) {
        return encryptData(decryptData(data, oldKey), newKey);
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
            Aes aes = new Aes(cryptoKey.getKey(), cryptoKey.getSalt());
            return aes.encrypt(data);
        } catch (Exception e) {
            Log.e("Cannot encrypt file data", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptData(byte[] data, CryptoKey cryptoKey) {
        try {
            Aes aes = new Aes(cryptoKey.getKey(), cryptoKey.getSalt());
            return aes.decrypt(data);
        } catch (Exception e) {
            Log.e("Cannot decrypt file data", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static EncryptedFileInfo encryptInfo(FileInfo fileInfo, CryptoKey cryptoKey) {
        Aes aes = new Aes(cryptoKey.getKey(), cryptoKey.getSalt());

        try {
            byte[] name = aes.encrypt(fileInfo.getName().getBytes(StandardCharsets.UTF_8));
            byte[] type = null;

            if (fileInfo.getType() != null) {
                type = aes.encrypt(fileInfo.getType().getBytes(StandardCharsets.UTF_8));
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
        Aes aes = new Aes(cryptoKey.getKey(), cryptoKey.getSalt());

        try {
            byte[] encryptedName = encryptedFileInfo.getEncryptedName();
            String decryptedName = new String(aes.decrypt(encryptedName));

            String decryptedType = null;

            if (encryptedFileInfo.getEncryptedType() != null) {
                byte[] encryptedType = encryptedFileInfo.getEncryptedType();
                decryptedType = new String(aes.decrypt(encryptedType));
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
