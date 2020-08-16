package com.notesr.controllers;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import com.notesr.models.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

public class Storage extends Application {

    public static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    public static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }

    public static boolean isFileExists(Context context, String filename) {
        File internalStorage = new File(context.getFilesDir(), Config.internalStoragePath);
        File file = new File(internalStorage, filename);

        return file.exists();
    }

    public static String readFile(Context context, String filename) {
        String result = "";

        File file = new File(context.getFilesDir(), Config.internalStoragePath);

        if(!file.exists()) {
            file.mkdir();
        }

        try {
            File gpxfile = new File(file, filename);
            FileReader reader = new FileReader(gpxfile);
            BufferedReader br = new BufferedReader(reader);

            result = br.readLine().replace("@###@~@###@", "\n");

            br.close();
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void writeFile(Context context, String filename, String data) {
        File internalStorage = new File(context.getFilesDir(), Config.internalStoragePath);

        if(!internalStorage.exists()) {
            internalStorage.mkdir();
        }

        try {
            File file = new File(internalStorage, filename);
            FileWriter writer = new FileWriter(file);

            writer.write(data.replace("\n", "@###@~@###@"));
            writer.flush();
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(Context context, String filename) {
        File internalStorage = new File(context.getFilesDir(), Config.internalStoragePath);
        File file = new File(internalStorage, filename);

        file.delete();
    }

    public static String externalReadFile(File file) {
        try {
            FileInputStream fstream = new FileInputStream(file);

            byte[] result = new byte[(int) file.length()];

            fstream.read(result);
            fstream.close();

            return new String(result);
        } catch (Exception e) {
            e.printStackTrace();

            return "";
        }
    }

    public static boolean externalWriteFile(File file, String filedata) {
        try {
            FileOutputStream fstream = new FileOutputStream(file);

            fstream.write(filedata.getBytes());
            fstream.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }
    }
}