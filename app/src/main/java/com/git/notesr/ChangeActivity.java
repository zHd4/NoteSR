package com.git.notesr;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;

public class ChangeActivity extends AppCompatActivity {
    public static int arg = 0;

    public static int CREATE_NOTE = 0;
    public static int EDIT_NOTE = 1;

    public static int n_id = 0;
    public static String n_label = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        final TextView titleText = findViewById(R.id.titleText);
        final TextView textText = findViewById(R.id.textText);

        final FloatingActionButton apply_button = findViewById(R.id.apply_button);

        if(arg == EDIT_NOTE){
            titleText.setText(n_label);
            textText.setText(MainActivity.notes_arr[n_id][1]);
        }

        apply_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!titleText.getText().toString().equals("") &&
                        !textText.getText().toString().equals("")) {
                    if(arg == CREATE_NOTE) {
                        String[][] note = { { titleText.getText().toString(),
                                textText.getText().toString() } };
                        MainActivity.notes_arr = concat(MainActivity.notes_arr, note);
                    }else{
                        MainActivity.notes_arr[n_id][0] = titleText.getText().toString();
                        MainActivity.notes_arr[n_id][1] = textText.getText().toString();
                    }

                    try {
                        Notes.SetNotes(getApplicationContext(), MainActivity.notes_arr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    startActivity(ActivityTools.GetIntent(getApplicationContext(),
                            MainActivity.class));
                }
            }
        });
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }
}