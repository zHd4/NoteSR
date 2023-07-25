package com.notesr.controllers;

import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.models.Config;
import com.notesr.views.AccessActivity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

@SuppressLint("Registered")
public class ActivityTools extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static ClipboardManager clipboard;

    public static Context getAppContext() {
        return context;
    }

    public static Intent getIntent(Context context, Class<?> _class) {
        Intent intent = new Intent(context, _class);
        return intent;
    }

    public static void showTextMessage(String text, int duration, Context context) {
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public static void checkReady(Context context, AppCompatActivity activity) {
        new ReadyChecker().checkReady(context, activity);
    }

    public static String keyToHex(String key) {
        StringBuilder result = new StringBuilder();
        byte[] keyBytes = key.getBytes();
        int buff = 4;

        for(int i=0; i<keyBytes.length; i++) {
            result.append(Integer.toHexString(keyBytes[i]));

            if(i != keyBytes.length - 1){
                result.append(" ");
            }

            buff--;

            if(buff == 0){
                result.append("\n");
                buff = 4;
            }
        }

        return result.toString();
    }

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
            String cryptoPassword = md5(Config.passwordCode);

            String encryptedKey = Base64.encodeToString(CryptoController.encrypt(
                    Config.cryptoKey.getBytes(),
                    md5(cryptoPassword),
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
                String cryptoPassword = md5(pin);
                
                String cryptoKey = new String(CryptoController.decrypt(
                        encryptedKey,
                        md5(cryptoPassword),
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

    public static String sha256(String text) {
        try {
            StringBuffer hexString = new StringBuffer();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);

                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String md5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");

            digest.update(s.getBytes());

            byte[] messageDigest = digest.digest();

            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));

            return hexString.toString();
        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String randomString(int len) {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();

        int randomLength = generator.nextInt(len);
        char tempChar;

        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }

        return randomStringBuilder.toString();
    }


    private static class ReadyChecker {
        public void checkReady(Context context, AppCompatActivity activity) {
            if(Config.cryptoKey == null || Config.passwordCode == null) {
                activity.startActivity(ActivityTools.getIntent(context, AccessActivity.class));
            }
        }
    }
}
