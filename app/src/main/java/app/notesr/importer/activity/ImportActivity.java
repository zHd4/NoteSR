package app.notesr.importer.activity;

import static app.notesr.core.util.ActivityUtils.disableBackButton;

import android.app.Activity;
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
import app.notesr.ActivityBase;
import app.notesr.note.activity.NotesListActivity;
import app.notesr.importer.service.ImportAndroidService;
import app.notesr.importer.service.ImportStatus;
import app.notesr.core.util.FileExifDataResolver;
import app.notesr.core.util.FilesUtils;

public final class ImportActivity extends ActivityBase {

    private static final String TAG = ImportActivity.class.getName();

    private Uri selectedFileUri;
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

        ImportBroadcastReceiver broadcastReceiver = new ImportBroadcastReceiver(
                this::onImportRunning,
                this::onImportFinished
        );

        LocalBroadcastManager.getInstance(this) .registerReceiver(broadcastReceiver,
                new IntentFilter(ImportAndroidService.BROADCAST_ACTION));

        boolean importRunning = isImportRunning();

        if (!importRunning) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.import_text);
        } else {
            actionBar.setTitle(R.string.importing);
            disableBackButton(this);
        }

        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                selectFileCallback());

        initViews(importRunning);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, NotesListActivity.class)
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

                    FileExifDataResolver resolver = new FileExifDataResolver(
                            getApplicationContext(), new FilesUtils(), selectedFileUri);
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

            disableBackButton(this);
            startImport();

            progressBar.setVisibility(View.VISIBLE);
            statusTextView.setVisibility(View.VISIBLE);
        };
    }

    private void onImportRunning(ImportStatus status) {
        if (status != null) {
            switch (status) {
                case DECRYPTING -> statusTextView.setText(R.string.decrypting_data);
                case IMPORTING -> statusTextView.setText(R.string.importing);
                case CLEANING_UP -> statusTextView.setText(R.string.wiping_temp_data);
                default -> throw new IllegalStateException("Unexpected value: " + status);
            }
        }
    }

    private void onImportFinished(ImportStatus status) {
        progressBar.setVisibility(View.INVISIBLE);

        switch (status) {
            case DECRYPTION_FAILED -> {
                statusTextView.setTextColor(getColor(android.R.color.holo_red_light));
                statusTextView.setText(R.string.cannot_decrypt_file);
            }
            case IMPORT_FAILED -> {
                statusTextView.setTextColor(getColor(android.R.color.holo_red_light));
                statusTextView.setText(R.string.cannot_import_data);
            }
            case DONE -> {
                statusTextView.setVisibility(View.INVISIBLE);
                startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
                finish();
            }
            default -> throw new IllegalStateException("Unexpected value: " + status);
        }

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.import_text);
    }

    private void startImport() {
        Intent intent = new Intent(this, ImportAndroidService.class)
                .setData(selectedFileUri);

        startForegroundService(intent);
    }

    private boolean isImportRunning() {
        return App.getContext().isServiceRunning(ImportAndroidService.class);
    }
}
