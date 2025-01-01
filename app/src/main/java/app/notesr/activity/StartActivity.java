package app.notesr.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.security.AuthActivity;

public class StartActivity extends ExtendedAppCompatActivity {
    private static final double BANNER_MARGIN_FOR_LOW_HEIGHT = 0.1;
    private static final double BANNER_MARGIN_FOR_LARGE_HEIGHT = 0.2;
    private static final int LOW_HEIGHT = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
        authActivityIntent.putExtra("mode", AuthActivity.Mode.CREATE_PASSWORD.toString());

        View.OnClickListener onGetStartedButtonClick = view -> startActivity(authActivityIntent);
        findViewById(R.id.getStartedButton).setOnClickListener(onGetStartedButtonClick);

        disableBackButton();
        placeBannerFront();
    }

    private void placeBannerFront() {
        ConstraintLayout layout = findViewById(R.id.bannerFrontLayout);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int screenHeight = displayMetrics.heightPixels;

        double percent = screenHeight > LOW_HEIGHT
                ? BANNER_MARGIN_FOR_LARGE_HEIGHT
                : BANNER_MARGIN_FOR_LOW_HEIGHT;

        int topMargin = (int) (percent * screenHeight);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone((ConstraintLayout) layout.getParent());

        constraintSet.connect(
                R.id.bannerFrontLayout,
                ConstraintSet.TOP,
                ConstraintSet.PARENT_ID,
                ConstraintSet.TOP,
                topMargin
        );

        constraintSet.applyTo((ConstraintLayout) layout.getParent());
    }
}
