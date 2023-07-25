package com.notesr.views;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.R;
import com.notesr.controllers.CryptoController;
import com.notesr.controllers.DatabaseController;
import com.notesr.controllers.ActivityTools;
import com.notesr.models.Config;
import com.notesr.controllers.StorageController;
import java.io.File;

public class ChooseFileActivity extends AppCompatActivity {

    public static boolean safeCalled = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.choosefile_activity);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        ActivityTools.checkReady(getApplicationContext(), this);

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
                String notesData = StorageController.externalReadFile(file);

                if (notesData.length() > 0) {
                    try {
                        String decryptedNotes = new String(CryptoController.decrypt(
                                Base64.decode(notesData, Base64.DEFAULT),
                                ActivityTools.sha256(Config.cryptoKey),
                                Base64.decode(Config.cryptoKey, Base64.DEFAULT)
                        ));

                        DatabaseController db = new DatabaseController(getApplicationContext());

                        db.importFromJsonString(getApplicationContext(), decryptedNotes);
                        startActivity(ActivityTools.getIntent(getApplicationContext(),
                                MainActivity.class));
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
