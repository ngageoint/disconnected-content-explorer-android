package mil.nga.dice;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import mil.nga.dice.report.ReportDropbox;
import mil.nga.dice.report.ReportManager;

import java.io.File;

public class DICE extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        File reportsDir = new File(getExternalFilesDir(null), "reports");
        Log.i("DICE", "initializing DICE with reports dir " + reportsDir.getAbsolutePath());
        ReportManager.initialize(this)
                .reportsDir(reportsDir)
                .finish();
        startService(new Intent(this, ReportDropbox.class));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopService(new Intent(this, ReportDropbox.class));
    }

}
