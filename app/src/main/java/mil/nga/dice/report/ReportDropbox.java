package mil.nga.dice.report;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;


public class ReportDropbox extends Service {

    private static final String TAG = ReportDropbox.class.getSimpleName();

    private static final long STABILITY_CHECK_INTERVAL = 250;
    private static final int MIN_STABILITY_CHECKS = 2;
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


    public static final String ACTION_IMPORT = "mil.nga.giat.dice.ReportDropbox.ACTION_IMPORT";


    private class FileStabilityCheck implements Callable<File> {
        private final File file;
        private int stableCount = 0;
        private long lastModified = 0;
        private long lastLength = 0;
        private FileStabilityCheck(File file) {
            this.file = file;
            lastLength = file.length();
            lastModified = file.lastModified();
        }
        private boolean fileIsStable() {
            return file.lastModified() == lastModified && file.length() == lastLength;
        }
        public void schedule() {
            fileChecking.schedule(this, STABILITY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }
        @Override
        public File call() {
            // TODO: handle zero-length file gracefully
            if (fileIsStable()) {
                if (++stableCount >= MIN_STABILITY_CHECKS) {
                    ReportManager.getInstance().processReports(Uri.fromFile(file));
                }
                else {
                    schedule();
                }
            }
            else {
                stableCount = 0;
                lastLength = file.length();
                lastModified = file.lastModified();
                schedule();
            }
            return null;
        }
    }

    private class CopyFileToDropbox extends AsyncTask<Uri, Void, Void> {
        @Override
        protected Void doInBackground(Uri... params) {
            Uri reportUri = params[0];
            String fileName = reportUri.toString();
            long fileSize = -1;
            if ("file".equals(reportUri.getScheme())) {
                File reportFile = new File(reportUri.getPath());
                fileName = reportFile.getName();
            }
            else if ("content".equals(reportUri.getScheme())) {
                Cursor reportInfo = getContentResolver().query(reportUri, null, null, null, null);
                int nameCol = reportInfo.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeCol = reportInfo.getColumnIndex(OpenableColumns.SIZE);
                reportInfo.moveToFirst();
                fileName = reportInfo.getString(nameCol);
                fileSize = reportInfo.getLong(sizeCol);
            }

            FileDescriptor fd = null;
            try {
                fd = getContentResolver().openFileDescriptor(reportUri, "r").getFileDescriptor();
            }
            catch (FileNotFoundException e) {
                // TODO: user feedback
                Log.e(TAG, "error opening file descriptor for report uri: " + reportUri, e);
                return null;
            }

            File destFile = new File(dropboxDir, fileName);
            FileChannel source = new FileInputStream(fd).getChannel();
            FileChannel dest = null;
            try {
                dest = new FileOutputStream(destFile).getChannel();
            }
            catch (FileNotFoundException e) {
                // TODO: user feedback
                Log.e(TAG, "error creating new report file for import: " + destFile, e);
                return null;
            }

            try {
                source.transferTo(0, fileSize, dest);
            }
            catch (IOException e) {
                Log.e(TAG, "error copying report file from " + reportUri + " to " + destFile, e);
            }
            finally {
                try {
                    source.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    dest.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private File dropboxDir;
    private FileObserver dropboxObserver;
    private ScheduledExecutorService fileChecking = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate() {
        Log.i(TAG, "creating report dropbox");
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
                Log.i(TAG, "file event: " + nameOfFileEvent(event) + "; " + fileName);
                File reportFile = new File(dropboxDir, fileName);
                new FileStabilityCheck(reportFile).schedule();
            }
        };
        findExistingReports();
        dropboxObserver.startWatching();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "starting command " + startId + "; intent " + String.valueOf(intent));

        dropboxObserver.startWatching();

        if (intent != null && ACTION_IMPORT.equals(intent.getAction())) {
            copyReportFileFrom(intent.getData());
        }

        return START_STICKY;
    }

    private void copyReportFileFrom(Uri reportUri) {
        new CopyFileToDropbox().executeOnExecutor(fileChecking, reportUri);
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
        Log.i(TAG, "finding existing reports in dir " + dropboxDir);
        File[] existingReports = dropboxDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
        for (File reportFile : existingReports) {
            Log.d(TAG, "found existing potential report " + reportFile);
            new FileStabilityCheck(reportFile).schedule();
        }
    }

}
