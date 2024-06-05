package com.peew.notesr.activity;

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
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.Note;
import com.peew.notesr.onclick.security.ChangePasswordOnClick;
import com.peew.notesr.onclick.notes.ExportNotesOnClick;
import com.peew.notesr.onclick.security.GenerateNewKeyOnClick;
import com.peew.notesr.onclick.security.LockOnClick;
import com.peew.notesr.onclick.notes.NewNoteOnClick;
import com.peew.notesr.onclick.notes.NoteOnClick;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MainActivity extends ExtendedAppCompatActivity {
    private final Map<Integer, Consumer<MainActivity>> menuItemsMap = new HashMap<>();

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
        CryptoManager cryptoManager = getCryptoManager();

        if (cryptoManager.isFirstRun()) {
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

        CryptoManager cryptoManager = getCryptoManager();

        executor.execute(() -> {
            if (cryptoManager.getCryptoKeyInstance() != null) {
                handler.post(progressDialog::show);

                NotesTable notesTable = App.getAppContainer().getNotesDatabase().getNotesTable();
                List<Note> notes = NotesCrypt.decrypt(notesTable.getAll());

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

    private CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}