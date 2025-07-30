package app.notesr.crypto;

import android.util.Log;
import app.notesr.App;
import app.notesr.dto.CryptoSecrets;
import app.notesr.model.EncryptedFileInfo;
import app.notesr.model.FileInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class FileCryptor {
    public static EncryptedFileInfo updateKey(EncryptedFileInfo fileInfo, CryptoSecrets oldKey, CryptoSecrets newKey) {
        return encryptInfo(decryptInfo(fileInfo, oldKey), newKey);
    }

    public static byte[] updateKey(byte[] data, CryptoSecrets oldKey, CryptoSecrets newKey) {
        return encryptData(decryptData(data, oldKey), newKey);
    }

    public static List<FileInfo> decryptInfo(List<EncryptedFileInfo> filesInfo) {
        return decryptInfo(filesInfo, getCryptoManager().getSecrets());
    }

    public static List<FileInfo> decryptInfo(
            List<EncryptedFileInfo> filesInfo,
            CryptoSecrets cryptoKey) {
        return filesInfo.stream()
                .map(fileInfo -> decryptInfo(fileInfo, cryptoKey))
                .collect(Collectors.toList());
    }

    public static byte[] encryptData(byte[] data) {
        return encryptData(data, getCryptoManager().getSecrets());
    }

    public static byte[] decryptData(byte[] data) {
        return decryptData(data, getCryptoManager().getSecrets());
    }

    public static EncryptedFileInfo encryptInfo(FileInfo fileInfo) {
        return encryptInfo(fileInfo, getCryptoManager().getSecrets());
    }

    public static FileInfo decryptInfo(EncryptedFileInfo encryptedFileInfo) {
        return decryptInfo(encryptedFileInfo, getCryptoManager().getSecrets());
    }

    public static byte[] encryptData(byte[] data, CryptoSecrets cryptoKey) {
        try {
            AesCryptor aesCryptor = new AesCryptor(cryptoKey.getKey(), cryptoKey.getSalt());
            return aesCryptor.encrypt(data);
        } catch (Exception e) {
            Log.e("Cannot encrypt file data", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptData(byte[] data, CryptoSecrets cryptoKey) {
        try {
            AesCryptor aesCryptor = new AesCryptor(cryptoKey.getKey(), cryptoKey.getSalt());
            return aesCryptor.decrypt(data);
        } catch (Exception e) {
            Log.e("Cannot decrypt file data", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static EncryptedFileInfo encryptInfo(FileInfo fileInfo, CryptoSecrets cryptoKey) {
        AesCryptor aesCryptor = new AesCryptor(cryptoKey.getKey(), cryptoKey.getSalt());

        try {
            byte[] name = aesCryptor.encrypt(fileInfo.getName().getBytes(StandardCharsets.UTF_8));
            byte[] type = null;

            if (fileInfo.getType() != null) {
                type = aesCryptor.encrypt(fileInfo.getType().getBytes(StandardCharsets.UTF_8));
            }

            byte[] thumbnail = null;

            if (fileInfo.getThumbnail() != null) {
                thumbnail = aesCryptor.encrypt(fileInfo.getThumbnail());
            }

            return EncryptedFileInfo.builder()
                    .id(fileInfo.getId())
                    .noteId(fileInfo.getNoteId())
                    .size(fileInfo.getSize())
                    .encryptedName(name)
                    .encryptedType(type)
                    .encryptedThumbnail(thumbnail)
                    .createdAt(fileInfo.getCreatedAt())
                    .updatedAt(fileInfo.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            Log.e("Cannot encrypt file info", e.toString());
            throw new RuntimeException(e);
        }
    }

    public static FileInfo decryptInfo(EncryptedFileInfo encryptedFileInfo, CryptoSecrets cryptoKey) {
        AesCryptor aesCryptor = new AesCryptor(cryptoKey.getKey(), cryptoKey.getSalt());

        try {
            byte[] encryptedName = encryptedFileInfo.getEncryptedName();
            String decryptedName = new String(aesCryptor.decrypt(encryptedName));

            String decryptedType = null;

            if (encryptedFileInfo.getEncryptedType() != null) {
                byte[] encryptedType = encryptedFileInfo.getEncryptedType();
                decryptedType = new String(aesCryptor.decrypt(encryptedType));
            }

            byte[] decryptedThumbnail = null;

            if (encryptedFileInfo.getEncryptedThumbnail() != null) {
                byte[] encryptedThumbnail = encryptedFileInfo.getEncryptedThumbnail();
                decryptedThumbnail = aesCryptor.decrypt(encryptedThumbnail);
            }

            return FileInfo.builder()
                    .id(encryptedFileInfo.getId())
                    .noteId(encryptedFileInfo.getNoteId())
                    .size(encryptedFileInfo.getSize())
                    .name(decryptedName)
                    .type(decryptedType)
                    .thumbnail(decryptedThumbnail)
                    .createdAt(encryptedFileInfo.getCreatedAt())
                    .updatedAt(encryptedFileInfo.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            Log.e("Cannot decrypt file info", e.toString());
            throw new RuntimeException(e);
        }
    }

    private static CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
