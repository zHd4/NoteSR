package com.peew.notesr.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.db.notes.tables.TableName;
import com.peew.notesr.models.Note;

import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends ExtendedAppCompatActivity {
    private final CryptoManager cryptoManager = CryptoManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configure();

        ListView notesView = findViewById(R.id.notes_list_view);

        fillNotesList(notesView);
        notesView.setOnItemClickListener(noteOnClick());
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

    private void fillNotesList(ListView notesView) {
        if (cryptoManager.getCryptoKeyInstance() != null) {
            NotesTable notesTable = (NotesTable)
                    NotesDatabase.getInstance()
                            .getTable(TableName.NOTES_TABLE);

            List<Note> notes = notesTable.getAll();
            List<String> notesNames = notes.stream().map(Note::getName).collect(Collectors.toList());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    App.getContext(),
                    R.layout.notes_list_item,
                    R.id.note_name_text_view,
                    notesNames);

            notesView.setAdapter(adapter);
        }
    }

    private AdapterView.OnItemClickListener noteOnClick() {
        return (adapter, view, position, id) -> {};
    }
}