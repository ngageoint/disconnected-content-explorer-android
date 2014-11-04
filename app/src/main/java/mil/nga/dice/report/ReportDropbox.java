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
import java.util.concurrent.*;


public class ReportDropbox extends Service {

    private static final String tag = ReportDropbox.class.getSimpleName();

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
                    throw new Error("unexpected error populating file event names", e);
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

    private static final long STABILITY_CHECK_INTERVAL = 500;
    private static final int MAX_STABILITY_CHECKS = 5;


    private class FileStabilityCheck implements Callable<File> {
        private final File file;
        private int checkCount = 0;
        private FileStabilityCheck(File file) {
            this.file = file;
        }
        private boolean fileIsStable() {
            return file.lastModified() < System.currentTimeMillis() - STABILITY_CHECK_INTERVAL;
        }
        private void schedule() {
            fileChecking.schedule(this, STABILITY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }
        @Override
        public File call() {
            if (fileIsStable()) {
                ReportManager.getInstance().processReports(file);
            }
            else if (++checkCount < MAX_STABILITY_CHECKS) {
                schedule();
            }
            return null;
        }
    }

    private File dropboxDir;
    private FileObserver dropboxObserver;
    private ScheduledExecutorService fileChecking = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate() {
        Log.i("ReportDropbox", "creating report dropbox");
        dropboxDir = new File(Environment.getExternalStorageDirectory(), "DICE");
        if (!dropboxDir.exists()) {
            dropboxDir.mkdirs();
        }
        if (!dropboxDir.isDirectory()) {
            throw new RuntimeException("dropbox is not a directory and could not be created: " + dropboxDir);
        }
        dropboxObserver = new FileObserver(dropboxDir.getAbsolutePath(),
                FileObserver.CREATE | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String fileName) {
                Log.i(tag, "file event: " + nameOfFileEvent(event) + "; " + fileName);
                File reportFile = new File(dropboxDir, fileName);
                new FileStabilityCheck(reportFile).schedule();
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
        fileChecking.shutdown();
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
