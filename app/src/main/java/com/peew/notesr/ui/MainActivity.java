package com.peew.notesr.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.adapter.NotesListAdapter;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.Note;
import com.peew.notesr.ui.auth.AuthActivity;
import com.peew.notesr.ui.manage.ImportNotesActivity;
import com.peew.notesr.ui.manage.KeyRecoveryActivity;
import com.peew.notesr.ui.manage.SearchNotesActivity;
import com.peew.notesr.ui.onclick.ChangePasswordOnClick;
import com.peew.notesr.ui.onclick.ExportNotesOnClick;
import com.peew.notesr.ui.onclick.GenerateNewKeyOnClick;
import com.peew.notesr.ui.onclick.LockOnClick;
import com.peew.notesr.ui.onclick.NewNoteOnClick;
import com.peew.notesr.ui.onclick.NoteOnClick;
import com.peew.notesr.ui.setup.StartActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainActivity extends ExtendedAppCompatActivity {
    private final Map<Integer, Consumer<MainActivity>> menuItemsMap = new HashMap<>();
    private final CryptoManager cryptoManager = CryptoManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        disableBackButton();
        configure();

        ListView notesView = findViewById(R.id.notes_list_view);
        TextView missingNotesLabel = findViewById(R.id.missing_notes_label);
        FloatingActionButton newNoteButton = findViewById(R.id.add_note_button);

        newNoteButton.setOnClickListener(new NewNoteOnClick(this));

        fillNotesList(notesView, missingNotesLabel);
        notesView.setOnItemClickListener(new NoteOnClick(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent searchActivityIntent = new Intent(App.getContext(), SearchNotesActivity.class);
        Intent importNotesActivityIntent = new Intent(App.getContext(), ImportNotesActivity.class);

        getMenuInflater().inflate(R.menu.menu_main, menu);

        menuItemsMap.put(R.id.lock_app_button, new LockOnClick());
        menuItemsMap.put(R.id.change_password_menu_item, new ChangePasswordOnClick());

        menuItemsMap.put(R.id.generate_new_key_menu_item, new GenerateNewKeyOnClick());
        menuItemsMap.put(R.id.export_menu_item, new ExportNotesOnClick());

        menuItemsMap.put(R.id.import_menu_item, action -> startActivity(importNotesActivityIntent));
        menuItemsMap.put(R.id.search_menu_item, action -> startActivity(searchActivityIntent));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Objects.requireNonNull(menuItemsMap.get(id)).accept(this);

        return true;
    }

    private void configure() {
        if(cryptoManager.isFirstRun()) {
            startActivity(new Intent(App.getContext(), StartActivity.class));
            finish();
        } else if (!cryptoManager.ready()) {
            Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
            authActivityIntent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);

            startActivity(authActivityIntent);
            finish();
        }

        if (cryptoManager.isBlocked()) {
            startActivity(new Intent(App.getContext(), KeyRecoveryActivity.class));
            finish();
        }
    }

    private void fillNotesList(ListView notesView, TextView missingNotesLabel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            if (cryptoManager.getCryptoKeyInstance() != null) {
                handler.post(progressDialog::show);

                NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();
                List<Note> notes = notesTable.getAll();

                if (!notes.isEmpty()) {
                    missingNotesLabel.setVisibility(View.INVISIBLE);
                    NotesListAdapter adapter = new NotesListAdapter(
                            App.getContext(),
                            R.layout.notes_list_item,
                            notes);

                    runOnUiThread(() -> notesView.setAdapter(adapter));
                }

                progressDialog.dismiss();
            }
        });
    }
}