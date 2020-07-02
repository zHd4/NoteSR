package com.git.notesr;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Storage extends Application {

    public static boolean isExternalStorageReadOnly()
    {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }

    public static boolean isExternalStorageAvailable()
    {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(extStorageState);
    }

    public static String ReadFile(Context mcoContext,String sFileName)
    {
        String result = "";

        File file = new File(mcoContext.getFilesDir(),"storage");
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File gpxfile = new File(file, sFileName);
            FileReader reader = new FileReader(gpxfile);
            BufferedReader br = new BufferedReader(reader);
            result = br.readLine().replace("@###@~@###@", "\n");
            br.close();
            reader.close();

        }catch (Exception e){
            e.printStackTrace();
        }

        return result;
    }

    public static void WriteFile(Context mcoContext,String sFileName, String sBody)
    {
        File file = new File(mcoContext.getFilesDir(),"storage");
        if(!file.exists()){
            file.mkdir();
        }

        try{
            File gpxfile = new File(file, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.write(sBody.replace("\n", "@###@~@###@"));
            writer.flush();
            writer.close();

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}