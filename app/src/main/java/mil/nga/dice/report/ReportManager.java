package mil.nga.dice.report;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.provider.OpenableColumns;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * TODO: modify to look for content roots in report dir instead of tying to source file
 */
public class ReportManager extends Service {

    private static final String TAG = ReportManager.class.getSimpleName();

	private static final int KEEP_ALIVE_TIME_SECONDS = 1;
	private static final int CORE_POOL_SIZE = 1;
	private static final int MAXIMUM_POOL_SIZE = 5;

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

    private static ReportManager instance;

    public static <T extends Context & ReportManagerClient> boolean bindTo(final T client) {
        Connection connection = new Connection(client);
        return client.bindService(new Intent(client, ReportManager.class), connection, 0);
    }

    // TODO: delete
    public static ReportManager getInstance() {
        return instance;
    }

    public static interface ReportManagerClient {
        void reportManagerConnected(Connection x);
        void reportManagerDisconnected();
    }

    public static final class Connection implements ServiceConnection {
        private ReportManagerClient client;
        private LocalBinder binding;
        private Connection(ReportManagerClient client) {
            this.client = client;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binding = (LocalBinder)service;
            client.reportManagerConnected(this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            client.reportManagerDisconnected();
        }
        public ReportManager getReportManager() {
            return binding.getService();
        }
    }

    private static String nameOfFileEvent(int event) {
        String name = fileEventNames.get(event);
        if (name == null) {
            name = String.valueOf(event);
        }
        return name;
    }

    private static class BackgroundRunnable implements Runnable {
        private final Runnable target;
        BackgroundRunnable(Runnable target){
            this.target = target;
        }
        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            target.run();
        }
    }

	static final int LOAD_FAILED = -1;
	static final int LOAD_COMPLETE = 1;

    public static final String EXTRA_REPORTS_DIR = "reports_dir";
    public static final String INTENT_UPDATE_REPORT_LIST = "mil.nga.giat.dice.ReportManager.UPDATE_REPORT_LIST";
    public static final String ACTION_IMPORT = "mil.nga.giat.dice.ReportManager.ACTION_IMPORT";

    private final LocalBinder localBinder = new LocalBinder();
	private final List<Report> reports = new ArrayList<>();
	private final List<Report> reportsView = Collections.unmodifiableList(reports);

	private Context context;
    private File dropboxDir;
    private File reportsDir;
    private FileObserver dropboxObserver;
    private ScheduledThreadPoolExecutor reportTasks;
	private Handler handler;

	public ReportManager() {
        super();

        if (instance != null) {
            throw new Error("too many ReportManager instances");
        }

        final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        ThreadFactory backgroundThreads = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return defaultThreadFactory.newThread(new BackgroundRunnable(r));
            }
        };
        reportTasks = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE, backgroundThreads);
        reportTasks.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        reportTasks.setKeepAliveTime(KEEP_ALIVE_TIME_SECONDS, TimeUnit.SECONDS);

        handler = new Handler(Looper.getMainLooper());
	}

    @Override
    public void onCreate() {
        Log.i(TAG, "creating report manager");
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "starting command " + startId + "; intent " + String.valueOf(intent));

        dropboxDir = new File(Environment.getExternalStorageDirectory(), "DICE");
        reportsDir = dropboxDir; // TODO: separate dir from dropboxDir
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        if (!reportsDir.isDirectory()) {
            throw new RuntimeException("content directory is not a directory or could not be created: " + reportsDir);
        }

        dropboxObserver = new FileObserver(dropboxDir.getAbsolutePath(),
                FileObserver.CREATE | FileObserver.MOVED_TO | FileObserver.DELETE | FileObserver.MOVED_FROM) {
            @Override
            public void onEvent(int event, String fileName) {
                Log.i(TAG, "file event: " + nameOfFileEvent(event) + "; " + fileName);
                File reportFile = new File(reportsDir, fileName);
                if (event == FileObserver.DELETE || event == FileObserver.MOVED_FROM) {
                    // TODO: handle deleted report file or dir
                }
                else if (event == FileObserver.CREATE || event == FileObserver.MOVED_TO) {
                    new FileStabilityCheck(reportFile).schedule();
                }
            }
        };

        findExistingReports();

        dropboxObserver.startWatching();

        if (intent != null && ACTION_IMPORT.equals(intent.getAction())) {
            copyReportFileFrom(intent.getData());
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        dropboxObserver.stopWatching();
        reportTasks.shutdown();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    /**
     * Return a live, read-only list of the processed reports.
     * @return
     */
    public List<Report> getReports() {
        return reportsView;
    }

    public Report getReportWithId(String id) {
        for (Report r : reports) {
            if (id.equals(r.getId())) {
                return r;
            }
        }
        return null;
    }

    public File getReportsDir() {
        return reportsDir;
    }

    /**
	 * Handle sending messages based on the state of a report task.
	 * @param report
	 * @param state
	 */
	public void handleState(Report report, int state) {
		switch (state) {
		case LOAD_COMPLETE:
			report.setEnabled(true);
			break;
		case LOAD_FAILED:
			report.setEnabled(false);
			report.setDescription("Problem loading report");
			break;
		}
		LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(INTENT_UPDATE_REPORT_LIST));
	}
	
	public void importReportFromUri(Uri reportUri) {
        String fileName = reportUri.toString();
        if ("file".equals(reportUri.getScheme())) {
            File reportFile = new File(reportUri.getPath());
            fileName = reportFile.getName();
        }
        else if ("content".equals(reportUri.getScheme())) {
            Cursor reportInfo = getContentResolver().query(reportUri, null, null, null, null);
            int nameCol = reportInfo.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            reportInfo.moveToFirst();
            fileName = reportInfo.getString(nameCol);
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        String simpleName = fileName.substring(0, fileName.lastIndexOf("."));

        Report report = new Report();
        report.setSourceFile(reportUri);
        report.setSourceFileName(fileName);
        report.setTitle(fileName);
        report.setDescription("Processing report ...");
        report.setEnabled(false);

        String mimeType = getContentResolver().getType(reportUri);
        if ("application/zip".equals(mimeType) || "zip".equalsIgnoreCase(extension)) {
            File unzipDir = new File(reportsDir, simpleName);
            report.setPath(unzipDir);
            addReport(report);
            // TODO: need to refactor this to use the threadpool properly
            startUnzip(report);
        }
        else if ("application/pdf".equals(mimeType) || "pdf".equalsIgnoreCase(extension)) {
            // TODO: need to look into more PDF options on android
            report.setEnabled(true);
            report.setDescription("");
            report.setPath(new File(reportsDir.getPath(), report.getSourceFileName()));
            // TODO: copy pdf to new location
            addReport(report);
        }
        else if (extension.equalsIgnoreCase("docx")) {
            // TODO: word files
        }
        else if (extension.equalsIgnoreCase("pptx")) {
            // TODO: powerpoint files
        }
        else if (extension.equalsIgnoreCase("xlsx")) {
            // TODO: excel files
        }
	}

	private void startUnzip(Report report) {
		reportTasks.execute(new ReportUnzipRunnable(report, this));
	}

    private Report getReportWithPath(File path) {
        for (Report r : reports) {
            if (r.getPath().equals(path)) {
                return r;
            }
        }
        return null;
    }

    private void addReport(Report report) {
        handler.post(new AddReportOnUIThread(report));
    }

    private void copyReportFileFrom(Uri reportUri) {
        new CopyFileToDropbox().executeOnExecutor(reportTasks, reportUri);
    }

    private void findExistingReports() {
        Log.i(TAG, "finding existing reports in dir " + reportsDir);
        File[] existingReports = reportsDir.listFiles(new FileFilter() {
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

    private class LocalBinder extends Binder {
        public ReportManager getService() {
            return ReportManager.this;
        }
    }

	private class AddReportOnUIThread implements Runnable {
		private final Report report;
		private AddReportOnUIThread(Report report) {
			this.report = report;
		}
		@Override
		public void run() {
			reports.add(report);
			LocalBroadcastManager.getInstance(ReportManager.this).sendBroadcastSync(new Intent(INTENT_UPDATE_REPORT_LIST));
		}
	}

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
            reportTasks.schedule(this, STABILITY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }
        @Override
        public File call() {
            // TODO: handle zero-length file gracefully
            if (fileIsStable()) {
                if (++stableCount >= MIN_STABILITY_CHECKS) {
                    importReportFromUri(Uri.fromFile(file));
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

            File destFile = new File(reportsDir, fileName);
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
}
