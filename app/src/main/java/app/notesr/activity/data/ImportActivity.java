package app.notesr.activity.data;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.activity.notes.NoteListActivity;
import app.notesr.service.data.importer.ImportResult;
import app.notesr.service.android.ImportService;
import app.notesr.utils.FileExifDataResolver;

public class ImportActivity extends ExtendedAppCompatActivity {

    private static final String TAG = ImportActivity.class.getName();

    private Uri selectedFileUri;
    private ImportResult result;

    private ActivityResultLauncher<Intent> fileChooserLauncher;
    private ActionBar actionBar;
    private Button selectFileButton;
    private ProgressBar progressBar;
    private TextView selectedFileTextView;
    private TextView statusTextView;
    private TextView infoTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        actionBar = getSupportActionBar();
        assert actionBar != null;

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(dataReceiver(), new IntentFilter("importDataBroadcast"));

        boolean importRunning = isImportRunning();

        if (!importRunning) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.import_text);
        } else {
            actionBar.setTitle(R.string.importing);
            disableBackButton();
        }

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                selectFileCallback());

        initViews(importRunning);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, NoteListActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initViews(boolean importRunning) {
        progressBar = findViewById(R.id.importProgressBar);
        statusTextView = findViewById(R.id.statusTextView);
        selectedFileTextView = findViewById(R.id.selectedFileTextView);
        selectFileButton = findViewById(R.id.selectFileToImportButton);
        infoTextView = findViewById(R.id.importInfoText);

        if (importRunning) {
            infoTextView.setVisibility(View.INVISIBLE);

            selectFileButton.setVisibility(View.INVISIBLE);
            selectFileButton.setEnabled(false);

            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setVisibility(View.VISIBLE);
        } else {
            selectFileButton.setOnClickListener(selectFileOnClick());

            Button startButton = findViewById(R.id.startImportButton);
            startButton.setOnClickListener(startButtonOnClick());
        }
    }

    private View.OnClickListener selectFileOnClick() {
        return view -> {
            Intent intent = new Intent()
                    .setType("application/x-trash")
                    .setAction(Intent.ACTION_GET_CONTENT);

            fileChooserLauncher.launch(Intent.createChooser(intent, getString(R.string.select_a_dump)));
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

                    infoTextView.setVisibility(View.INVISIBLE);

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

    private View.OnClickListener startButtonOnClick() {
        return view -> {
            if (selectedFileUri == null) {
                RuntimeException e = new NullPointerException("File Uri is null");

                Log.e(TAG, "NullPointerException", e);
                throw e;
            }

            view.setEnabled(false);
            view.setVisibility(View.INVISIBLE);

            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getString(R.string.importing));

            selectFileButton.setEnabled(false);
            selectFileButton.setVisibility(View.INVISIBLE);

            selectedFileTextView.setVisibility(View.INVISIBLE);

            disableBackButton();
            startImport();

            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setVisibility(View.VISIBLE);
        };
    }

    private BroadcastReceiver dataReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra("status");
                String resultStr = intent.getStringExtra("result");

                result = resultStr != null ? ImportResult.valueOf(resultStr) : null;

                if (result != null && result != ImportResult.NONE) {
                    onImportFinished();
                } else {
                    statusTextView.setText(status);
                }
            }
        };
    }

    private void onImportFinished() {
        if (result == ImportResult.FINISHED_SUCCESSFULLY) {
            startActivity(new Intent(getApplicationContext(), NoteListActivity.class));
            finish();
        } else {
            findViewById(R.id.importCannotBeCanceledLabel).setVisibility(View.INVISIBLE);

            progressBar.setVisibility(View.INVISIBLE);
            statusTextView.setTextColor(getColor(android.R.color.holo_red_light));

            if (result == ImportResult.DECRYPTION_FAILED) {
                statusTextView.setText(R.string.cannot_decrypt_file);
            } else if (result == ImportResult.IMPORT_FAILED) {
                statusTextView.setText(R.string.cannot_import_data);
            }

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.import_text);
        }
    }

    private void startImport() {
        Intent intent = new Intent(this, ImportService.class)
                .setData(selectedFileUri);

        startForegroundService(intent);
    }

    private boolean isImportRunning() {
        return App.getContext().isServiceRunning(ImportService.class);
    }
}