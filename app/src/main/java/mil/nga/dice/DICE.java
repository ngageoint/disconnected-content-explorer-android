package mil.nga.dice;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.webkit.WebView;

import com.google.android.gms.maps.MapsInitializer;

import java.io.File;

import mil.nga.dice.map.BackgroundTileProvider;
import mil.nga.dice.map.OfflineMap;
import mil.nga.dice.report.ReportManager;

public class DICE extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        File appRoot = getExternalFilesDir(null);
        if (appRoot == null) {
            throw new Error("failed to obtain app directory on external storage; " +
                    "external storage state is " + Environment.getExternalStorageState());
        }
        File reportsDir = new File(appRoot, "reports");
        if (!reportsDir.isDirectory() && !reportsDir.mkdirs()) {
            throw new Error("failed to create reports directory " + reportsDir);
        }

        Log.i("DICE", "initializing DICE with reports dir " + reportsDir.getAbsolutePath());

        MapsInitializer.initialize(this);
        BackgroundTileProvider.initialize(this);
        OfflineMap.initialize(this);
        ReportManager.initialize(this);
    }

}
