package com.git.notesr;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class ChooseFileActivity extends AppCompatActivity {

    public static boolean safeCalled = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choosefile_activity);

        ActivityTools.requirePermission(ChooseFileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (safeCalled) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, 1);
        } else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        safeCalled = false;

        if (resultCode == RESULT_OK) {
            try {
                String src = getPath(data.getData());

                if (src.contains("msf")) {
                    Uri uri = data.getData();
                    src = uri.getPath();
                }

                src = src.replace("/document/raw:", "");

                File file = new File(src);
                String notesData = Storage.externalReadFile(file);

                if (notesData.length() > 0) {
                    try {
                        String decryptedNotes = AES.Decrypt(
                                notesData,
                                Base64.decode(Config.aesKey, Base64.DEFAULT)
                        );

                        Database db = new Database(getApplicationContext());

                        db.importFromJsonString(decryptedNotes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        finish();
    }

    private String getPath(Uri uri) {

        String path;
        String[] projection = { MediaStore.Files.FileColumns.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor == null){
            path = uri.getPath();
        } else {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            path = cursor.getString(column_index);
            cursor.close();
        }

        return ((path == null || path.isEmpty()) ? (uri.getPath()) : path);
    }
}
