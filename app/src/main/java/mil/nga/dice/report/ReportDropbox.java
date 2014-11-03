package mil.nga.dice.report;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;


public class ReportDropbox extends Service {

    private static final String tag = ReportDropbox.class.getSimpleName();

    private final File dropboxDir = new File(Environment.getExternalStorageDirectory(), "DICE");

    private FileObserver dropboxObserver;

    private static final Map<Integer, String> fileEventNames = new HashMap<>();
    static {
        for (Field f : FileObserver.class.getFields()) {
            if (
                    Modifier.isStatic(f.getModifiers()) &&
                    Modifier.isPublic(f.getModifiers()) &&
                    Modifier.isFinal(f.getModifiers()) &&
                    int.class.equals(f.getType()))
            {
                try {
                    fileEventNames.put(f.getInt(FileObserver.class), f.getName());
                }
                catch (Exception e) {
                }
            }
        }
    }

    private static String nameOfFileEvent(int event) {
        String name = fileEventNames.get(event);
        if (name == null) {
            name = String.valueOf(event);
        }
        return name;
    }

    @Override
    public void onCreate() {
        Log.i("ReportDropbox", "creating report dropbox");
        if (!dropboxDir.exists()) {
            dropboxDir.mkdirs();
        }
        if (!dropboxDir.isDirectory()) {
            throw new RuntimeException("dropbox is not a directory: " + dropboxDir);
        }
        dropboxObserver = new FileObserver(dropboxDir.getAbsolutePath(),
                FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String fileName) {
                Log.i(tag, "file event: " + nameOfFileEvent(event) + "; " + fileName);
                ReportManager.getInstance().processReports(new File(dropboxDir, fileName));
            }
        };
        findExistingReports();
        dropboxObserver.startWatching();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ReportDropbox", "starting command " + startId + (intent != null ? "; intent " + intent.toString() : ""));
        if (intent != null && intent.getDataString() != null) {
            String reportPath = intent.getDataString();
            ReportManager.getInstance().processReports(new File(reportPath));
        }
        dropboxObserver.startWatching();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        dropboxObserver.stopWatching();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void findExistingReports() {
        ReportManager.getInstance().processReports(dropboxDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        }));
    }

}
