package notesr.activity.files.viewer;

import android.os.Bundle;
import notesr.R;

public class OpenUnknownFileActivity extends BaseFileViewerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_unknown_file);
    }
}