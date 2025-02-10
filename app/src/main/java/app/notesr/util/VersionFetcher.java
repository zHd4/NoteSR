package app.notesr.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class VersionFetcher {
    public static String fetchVersionName(Context context, boolean removeDot) throws
            PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);

        return removeDot ? packageInfo.versionName.replace(".", "") :
                packageInfo.versionName;
    }
}
