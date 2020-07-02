package com.git.notesr;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class GenkeysActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak") // may be error
    public static Context context;
    private static String visualKey = "";
    public static ClipboardManager clipboard;

    public static Context getAppContext() {
        return context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        setContentView(R.layout.genkeys_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        TextView pKey = findViewById(R.id.pkeyView);
        Button copyTCButton = findViewById(R.id.copyToClipboardButton);
        Button nextGKButton = findViewById(R.id.nextGenkeysButton);

        copyTCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", visualKey);
                clipboard.setPrimaryClip(clip);

                ShowTextMessage("Copied!");
            }
        });

        nextGKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessActivity.operation = AccessActivity.CREATE_PIN;
                StartAccessActivity();
            }
        });

        try {
            String randKey = randomString(2048);
            Config.aesKey = Base64.encodeToString(AES.GenKey(randKey, md5(randKey)), Base64.DEFAULT);

            visualKey = keyToHex(randKey);

            pKey.setText(String.format("%s...", visualKey.substring(0, 100)));
        } catch (Exception e) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    public static void SaveKey() throws Exception {
        if (Storage.isExternalStorageAvailable() && !Storage.isExternalStorageReadOnly()) {
            String aesPassword = md5(Config.pinCode);

            Storage.WriteFile(getAppContext(),"key.bin", AES.Encrypt(Config.aesKey, AES.GenKey(aesPassword, md5(aesPassword))));
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    public static boolean GetKeys(String pin) {

        if (Storage.isExternalStorageAvailable() && !Storage.isExternalStorageReadOnly()) {
            String encryptedKey = Storage.ReadFile(getAppContext(),"key.bin");

            try{
                String aesPassword = md5(pin);
                String aesKey = AES.Decrypt(encryptedKey, AES.GenKey(aesPassword, md5(aesPassword)));

                Config.aesKey = aesKey;
                Config.pinCode = pin;

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

    private void StartAccessActivity() {
        Intent saIntent = new Intent(this, AccessActivity.class);
        startActivity(saIntent);
    }

    public void ShowTextMessage(String text) {
        int duration = Toast.LENGTH_SHORT;

        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, text, duration);

        toast.show();
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
}