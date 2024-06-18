package com.peew.notesr.activity.files.viewer;

import android.content.Intent;
import android.view.MenuItem;
import com.peew.notesr.App;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.activity.files.AssignmentsListActivity;
import com.peew.notesr.model.File;

public class FileViewerActivityBase extends AppCompatActivityExtended {
    protected File file;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

            intent.putExtra("note_id", file.getNoteId());
            startActivity(intent);

            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
