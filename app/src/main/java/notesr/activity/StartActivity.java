package notesr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import notesr.App;
import notesr.R;
import notesr.activity.security.AuthActivity;

public class StartActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.CREATE_PASSWORD_MODE);

        View.OnClickListener onGetStartedButtonClick = view -> startActivity(authActivityIntent);
        findViewById(R.id.getStartedButton).setOnClickListener(onGetStartedButtonClick);

        disableBackButton();
    }
}
