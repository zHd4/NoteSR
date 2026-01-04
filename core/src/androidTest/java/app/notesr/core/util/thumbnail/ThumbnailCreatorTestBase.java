/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util.thumbnail;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Size;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ThumbnailCreatorTestBase {
    protected static File getFixture(String fileName) throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        Context instrumentationContext = InstrumentationRegistry.getInstrumentation().getContext();

        File tempFixture = new File(context.getCacheDir(), fileName);

        try(InputStream inputStream = instrumentationContext.getAssets().open(fileName);
            FileOutputStream outputStream = new FileOutputStream(tempFixture)) {

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        return tempFixture;
    }

    protected static Size getImageSize(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        int width = options.outWidth;
        int height = options.outHeight;

        return new Size(width, height);
    }
}
