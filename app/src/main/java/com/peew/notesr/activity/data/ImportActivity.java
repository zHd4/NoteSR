package com.peew.notesr.activity.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

public class ImportActivity extends ExtendedAppCompatActivity {

    private ActionBar actionBar;

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
                if (result.getData() != null) {
                    TextView infoTextView = findViewById(R.id.importInfoText);

                    Button startButton = findViewById(R.id.startImportButton);
                    Button selectFileButton = findViewById(R.id.selectFileToImportButton);

                    infoTextView.setVisibility(View.INVISIBLE);

                    startButton.setVisibility(View.VISIBLE);
                    startButton.setEnabled(true);

                    selectFileButton.setVisibility(View.INVISIBLE);
                    selectFileButton.setEnabled(false);
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