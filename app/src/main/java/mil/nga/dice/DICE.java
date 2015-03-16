package mil.nga.dice;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.maps.MapsInitializer;

import mil.nga.dice.map.BackgroundTileProvider;
import mil.nga.dice.map.OfflineMap;
import mil.nga.dice.report.ReportDropbox;
import mil.nga.dice.report.ReportManager;

import java.io.File;

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

        ReportManager.initialize(this)
                .reportsDir(reportsDir)
                .finish();
        MapsInitializer.initialize(this);
        BackgroundTileProvider.initialize(this);
        OfflineMap.initialize(this);

        startService(new Intent(this, ReportDropbox.class));
    }



    @Override
    public void onTerminate() {
        super.onTerminate();
        stopService(new Intent(this, ReportDropbox.class));
    }

}
