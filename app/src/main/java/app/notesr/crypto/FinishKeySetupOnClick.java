package app.notesr.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import app.notesr.data.ReEncryptionActivity;
import app.notesr.note.NoteListActivity;
import app.notesr.service.crypto.KeySetupService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FinishKeySetupOnClick implements View.OnClickListener {
    private final Activity parentActivity;
    private final KeySetupService keySetupService;
    private final KeySetupMode mode;
    
    @Override
    public void onClick(View view) {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
        }
    }

    private void proceedFirstRun() {
        try {
            Context context = parentActivity.getApplicationContext();

            keySetupService.apply();
            parentActivity.startActivity(new Intent(context, NoteListActivity.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void proceedRegeneration() {
        Context context = parentActivity.getApplicationContext();
        Intent intent = new Intent(context, ReEncryptionActivity.class)
                .putExtra("newCryptoKey", keySetupService.getCryptoKey());

        parentActivity.startActivity(intent);
        parentActivity.finish();
    }
}
