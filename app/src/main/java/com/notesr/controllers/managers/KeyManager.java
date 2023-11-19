package com.notesr.controllers.managers;

import android.content.Context;
import android.util.Base64;

import com.notesr.controllers.StorageController;
import com.notesr.controllers.crypto.CryptoController;
import com.notesr.models.Config;

import java.util.Arrays;

public class KeyManager {
    public static String hexToKey(final String hex) {
        byte[] keyBytes = new byte[0];

        String hexString = hex.replace("\n", "").replace(" ", "");

        for(int i = 0; i < hexString.length(); i++) {
            StringBuilder element = new StringBuilder();

            if(i < hexString.length() - 1) {
                element.append(hexString.toCharArray()[i]);
                element.append(hexString.toCharArray()[i + 1]);

                i++;
            } else {
                element = new StringBuilder(String.valueOf(hexString.toCharArray()[i]));
            }

            keyBytes = Arrays.copyOf(keyBytes, keyBytes.length + 1);
            keyBytes[keyBytes.length - 1] = (byte) Integer.parseInt(element.toString(), 16);
        }

        return new String(keyBytes);
    }

    public static void saveKey(Context context) throws Exception {
        if (StorageController.isExternalStorageAvailable() && !StorageController.isExternalStorageReadOnly()) {
            String cryptoPassword = HashManager.toMd5String(Config.passwordCode);

            String encryptedKey = Base64.encodeToString(CryptoController.encrypt(
                    Config.cryptoKey.getBytes(),
                    HashManager.toMd5String(cryptoPassword),
                    CryptoController.genKey(cryptoPassword)), Base64.DEFAULT);

            StorageController.writeFile(context,Config.keyBinFileName, encryptedKey);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    public static boolean getKeys(String pin, Context context) {

        if (StorageController.isExternalStorageAvailable() && !StorageController.isExternalStorageReadOnly()) {
            byte[] encryptedKey = Base64.decode(StorageController.readFile(context,Config.keyBinFileName),
                    Base64.DEFAULT);

            try{
                String cryptoPassword = HashManager.toMd5String(pin);

                String cryptoKey = new String(CryptoController.decrypt(
                        encryptedKey,
                        HashManager.toMd5String(cryptoPassword),
                        CryptoController.genKey(cryptoPassword)
                ));

                Config.cryptoKey = cryptoKey;
                Config.passwordCode = pin;

                return true;

            } catch (Exception e) {
                e.printStackTrace();

                return false;
            }

        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

            return false;
        }
    }
}
