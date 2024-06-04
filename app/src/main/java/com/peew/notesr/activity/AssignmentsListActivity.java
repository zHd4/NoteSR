package com.peew.notesr.activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.peew.notesr.R;

public class AssignmentsListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignments_list);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.files);

//        fillTable();
    }

//    private void fillTable() {
//        FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
//
//        if (filesTable.getByNoteId(1).isEmpty()) {
//            File file1 = new File("file1.txt", "foo".getBytes(StandardCharsets.UTF_8));
//            File file2 = new File("file2.txt", "bar".getBytes(StandardCharsets.UTF_8));
//            File file3 = new File("file3.txt", "baz".getBytes(StandardCharsets.UTF_8));
//
//            file1.setNoteId(1L);
//            file2.setNoteId(1L);
//            file3.setNoteId(1L);
//
//            filesTable.save(FilesCrypt.encrypt(file1));
//            filesTable.save(FilesCrypt.encrypt(file2));
//            filesTable.save(FilesCrypt.encrypt(file3));
//        }
//
//        List<File> files = FilesCrypt.decrypt(filesTable.getByNoteId(1L));
//        System.out.println();
//    }
}