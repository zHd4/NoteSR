package com.peew.notesr.onclick.security;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import androidx.core.content.ContextCompat;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.activity.security.SetupKeyActivity;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.DataBlocksTable;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.DataBlock;
import com.peew.notesr.model.EncryptedFileInfo;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinishKeySetupOnClick implements View.OnClickListener {
    private final SetupKeyActivity activity;
    private final int mode;
    private final String password;
    private CryptoKey key;

    public FinishKeySetupOnClick(SetupKeyActivity activity, int mode, String password, CryptoKey key) {
        this.activity = activity;
        this.mode = mode;
        this.password = password;
        this.key = key;
    }

    @Override
    public void onClick(View v) {
        EditText importKeyField = activity.findViewById(R.id.import_key_field);
        String hexKeyToImport = importKeyField.getText().toString();

        if (!hexKeyToImport.isBlank()) {
            try {
                key = CryptoTools.hexToCryptoKey(hexKeyToImport, password);
            } catch (Exception e) {
                Log.e("hexToCryptoKey" , e.toString());
                proceedKeyImportFail(importKeyField);
                return;
            }
        }

        if (mode == SetupKeyActivity.FIRST_RUN_MODE) {
            try {
                getCryptoManager().applyNewKey(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
        } else if (mode == SetupKeyActivity.REGENERATION_MODE) {
            proceedRegeneration();
        }
    }

    private void proceedRegeneration() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

        executor.execute(() -> {
            handler.post(() -> {
                builder.setView(R.layout.progress_dialog_re_encryption);
                builder.setCancelable(false);
                builder.create().show();
            });

            try {
                CryptoManager cryptoManager = getCryptoManager();
                CryptoKey oldCryptoKey = cryptoManager.getCryptoKeyInstance().copy();

                cryptoManager.applyNewKey(key);
                updateEncryptedData(oldCryptoKey, key);

                activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
                activity.finish();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void updateEncryptedData(CryptoKey oldKey, CryptoKey newKey) {
        NotesTable notesTable = App.getAppContainer().getNotesDatabase().getTable(NotesTable.class);
        FilesTable filesTable = App.getAppContainer().getNotesDatabase().getTable(FilesTable.class);

        DataBlocksTable dataBlocksTable = App.getAppContainer().getNotesDatabase().getTable(DataBlocksTable.class);

        notesTable.getAll().forEach(note -> {
            notesTable.save(NotesCrypt.updateKey(note, oldKey, newKey));

            filesTable.getByNoteId(note.getId()).forEach(fileInfo -> {
                EncryptedFileInfo updatedFileInfo = FilesCrypt.updateKey(fileInfo, oldKey, newKey);
                Set<Long> blockIds = dataBlocksTable.getBlocksIdsByFileId(updatedFileInfo.getId());

                for (Long blockId : blockIds) {
                    DataBlock block = dataBlocksTable.get(blockId);

                    block.setData(FilesCrypt.updateKey(block.getData(), oldKey, newKey));
                    dataBlocksTable.save(block);
                }

                filesTable.save(updatedFileInfo);
            });
        });
    }

    private void proceedKeyImportFail(EditText importKeyField) {
        int importFailedColor = ContextCompat.getColor(
                App.getContext(),
                R.color.key_import_failed_color);

        ColorFilter colorFilter = new BlendModeColorFilter(
                importFailedColor,
                BlendMode.SRC_ATOP);

        importKeyField.getBackground().setColorFilter(colorFilter);
    }

    private CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }
}
