package com.peew.notesr.activity.data;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.service.ImportService;
import com.peew.notesr.tools.FileExifDataResolver;

public class ImportActivity extends ExtendedAppCompatActivity {

    private ActionBar actionBar;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        actionBar = getSupportActionBar();
        assert actionBar != null;

        if (importRunning()) {
            actionBar.setTitle(R.string.importing);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.import_text);
        }

        Button selectFileButton = findViewById(R.id.selectFileToImportButton);
        selectFileButton.setOnClickListener(selectFileOnClick());
    }

    private View.OnClickListener selectFileOnClick() {
        return view -> {
            ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    selectFileCallback());

            Intent intent = new Intent()
                    .setType("application/x-trash")
                    .setAction(Intent.ACTION_GET_CONTENT);

            resultLauncher.launch(Intent.createChooser(intent, getString(R.string.select_a_dump)));
        };
    }

    private ActivityResultCallback<ActivityResult> selectFileCallback() {
        return result -> {
            int resultCode = result.getResultCode();

            if (resultCode == Activity.RESULT_OK) {
                Intent data = result.getData();

                if (data != null) {
                    selectedFileUri = data.getData();

                    FileExifDataResolver resolver = new FileExifDataResolver(selectedFileUri);
                    String filename = resolver.getFileName();

                    TextView infoTextView = findViewById(R.id.importInfoText);
                    infoTextView.setVisibility(View.INVISIBLE);

                    TextView selectedFileTextView = findViewById(R.id.selectedFileTextView);

                    selectedFileTextView.setVisibility(View.VISIBLE);
                    selectedFileTextView.setText(filename);

                    Button startButton = findViewById(R.id.startImportButton);

                    startButton.setVisibility(View.VISIBLE);
                    startButton.setEnabled(true);
                } else {
                    throw new RuntimeException("Activity result is 'OK', but data not provided");
                }
            }
        };
    }

    private boolean importRunning() {
        return App.getContext().serviceRunning(ImportService.class);
    }
}