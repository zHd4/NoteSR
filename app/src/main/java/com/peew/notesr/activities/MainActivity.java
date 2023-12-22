package com.peew.notesr.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.adapters.NotesListAdapter;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.models.Note;
import com.peew.notesr.models.NoteItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainActivity extends ExtendedAppCompatActivity {
    private final Map<Integer, Consumer<?>> menuItemsMap = new HashMap<>();
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

        newNoteButton.setOnClickListener(newNoteOnClick());

        fillNotesList(notesView, missingNotesLabel);
        notesView.setOnItemClickListener(noteOnClick());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Intent searchActivityIntent = new Intent(App.getContext(), SearchNotesActivity.class);

        getMenuInflater().inflate(R.menu.menu_main, menu);

        menuItemsMap.put(R.id.lock_app_button, action -> lockOnClick());
        menuItemsMap.put(R.id.search_menu_item, action -> startActivity(searchActivityIntent));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Objects.requireNonNull(menuItemsMap.get(id)).accept(null);

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
        if (cryptoManager.getCryptoKeyInstance() != null) {
            NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();
            List<Note> notes = notesTable.getAll();

            if (!notes.isEmpty()) {
                missingNotesLabel.setVisibility(View.INVISIBLE);

                List<NoteItem> items = notes.stream()
                        .map(note -> new NoteItem(note.name(), note.text()))
                        .collect(Collectors.toList());

                NotesListAdapter adapter = new NotesListAdapter(
                        App.getContext(),
                        R.layout.notes_list_item,
                        items);

                notesView.setAdapter(adapter);
            }
        }
    }

    private AdapterView.OnItemClickListener noteOnClick() {
        return (adapter, view, position, id) -> {
            Intent noteOpenActivtyIntent = new Intent(App.getContext(), NoteOpenActivity.class);

            noteOpenActivtyIntent.putExtra("mode", NoteOpenActivity.EDIT_NOTE_MODE);
            noteOpenActivtyIntent.putExtra("note_id", id);

            startActivity(noteOpenActivtyIntent);
        };
    }

    private View.OnClickListener newNoteOnClick() {
        return view -> {
            if (cryptoManager.getCryptoKeyInstance() != null) {
                NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();

                long id = notesTable.getNewNoteId();
                Intent noteOpenActivtyIntent = new Intent(App.getContext(), NoteOpenActivity.class);

                noteOpenActivtyIntent.putExtra("mode", NoteOpenActivity.NEW_NOTE_MODE);
                noteOpenActivtyIntent.putExtra("note_id", id);

                startActivity(noteOpenActivtyIntent);
            }
        };
    }

    private void lockOnClick() {
        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.AUTHORIZATION_MODE);

        cryptoManager.destroyKey();

        startActivity(authActivityIntent);
        finish();
    }
}