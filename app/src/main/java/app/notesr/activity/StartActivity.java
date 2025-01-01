package app.notesr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.security.AuthActivity;

public class StartActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CREATE_PASSWORD.toString());

        View.OnClickListener onGetStartedButtonClick = view -> startActivity(authActivityIntent);
        findViewById(R.id.getStartedButton).setOnClickListener(onGetStartedButtonClick);

        disableBackButton();
    }
}
