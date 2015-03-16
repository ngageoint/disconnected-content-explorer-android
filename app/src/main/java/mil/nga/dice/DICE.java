package mil.nga.dice;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.MapsInitializer;

import java.io.File;

import mil.nga.dice.map.BackgroundTileProvider;
import mil.nga.dice.map.OfflineMap;
import mil.nga.dice.report.ReportManager;

public class DICE extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

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

        Intent reportManager = new Intent(this, ReportManager.class);
        startService(reportManager);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopService(new Intent(this, ReportManager.class));
    }

}
