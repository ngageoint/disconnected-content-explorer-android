package mil.nga.dice.report;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.dice.R;

/**
 * TODO: modify to look for content roots in report dir instead of tying to source file
 */
public class ReportManager implements ReportImportCallbacks {

    private static final String TAG = ReportManager.class.getSimpleName();

    private static final String DELETE_DIR_PREFIX = ".deleting.";

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

    private static final Set<String> supportedReportFileTypes;
    static {
        Set<String> types = new TreeSet<>(Arrays.asList(new String[] {
                "zip", "application/zip",
                "pdf", "application/pdf",
                "doc",
                "docx",
                "xls",
                "xlsx",
                "ppt",
                "pptx"
        }));
        supportedReportFileTypes = Collections.unmodifiableSet(types);
    }

    public static synchronized ReportManager initialize(Context context) {
        return instance = new ReportManager(context);
    }

    public static ReportManager getInstance() {
        if (instance == null) {
            throw new Error("ReportManager has not been properly initialized");
        }
        return instance;
    }

    private static ReportManager instance;

    private static String nameOfFileEvent(int event) {
        String name = fileEventNames.get(event);
        if (name == null) {
            name = String.valueOf(event);
        }
        return name;
    }

    public static final String INTENT_UPDATE_REPORT_LIST = "mil.nga.giat.dice.ReportManager.UPDATE_REPORT_LIST";

	private final List<Report> reports = new ArrayList<>();
	private final List<Report> reportsView = Collections.unmodifiableList(reports);

	private Context context;
    private File dropboxDir;
    private File reportsDir;
    private FileObserver dropboxObserver;
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService importExecutor;
	private Handler handler;

	public ReportManager(Context context) {
        super();

        if (instance != null) {
            throw new Error("too many ReportManager instances");
        }

        this.context = context;


        final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
        ThreadFactory backgroundThreads = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = defaultThreadFactory.newThread(r);
                t.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
                return t;
            }
        };

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(backgroundThreads);
        int coreThreadCount = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "initializing import thread pool with " + coreThreadCount + " core threads");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreadCount, coreThreadCount,
                30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), backgroundThreads);
        importExecutor = executor;

        handler = new Handler(Looper.getMainLooper());

        dropboxDir = new File(Environment.getExternalStorageDirectory(), "DICE");
        // TODO: separate reportsDir from dropboxDir to avoid too many FileObserver events and confusion
        reportsDir = dropboxDir;
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        if (!reportsDir.isDirectory()) {
            throw new RuntimeException("content directory is not a directory or could not be created: " + reportsDir);
        }

        findExistingReports();

        dropboxObserver = new DropboxObserver();
        dropboxObserver.startWatching();
	}

    public void destroy() {
        Log.i(TAG, "destroying ReportManager");

        scheduledExecutor.shutdown();
        importExecutor.shutdown();
        dropboxObserver.stopWatching();
        dropboxObserver = null;
        scheduledExecutor = null;
        handler = null;
        instance = null;
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

    /**
     * Add a report to DICE from the given {@link android.net.Uri}.  If the Uri points to a zip file, extract it
     * to the reports directory.  If the Uri points to some other kind of regular file, copy it to the reports
     * directory if necessary.
     * {@link android.support.v4.content.LocalBroadcastManager#sendBroadcast(android.content.Intent) broadcast}
     * a {@link #INTENT_UPDATE_REPORT_LIST} notification.
     *
     * @param reportUri
     */
	public void importReportFromUri(Uri reportUri) {
        if (!uriCouldBeReport(reportUri)) {
            return;
        }
        Report report = addNewReportForUri(reportUri);
        continueImport(report);
	}

    // TODO: do something with this
    public void deleteReport(Report report) {
        File reportPath = report.getPath();
        if (reportPath.isFile()) {
            if (!reportPath.delete()) {
                Log.e(TAG, "failed to delete report file: " + reportPath);
            }
        }
        else if (reportPath.isDirectory()) {
            File deleteDir = new File(reportPath.getParent(), DELETE_DIR_PREFIX + reportPath.getName());
            if (!reportPath.renameTo(deleteDir)) {
                Log.e(TAG, "failed to rename report directory for deleting: " + reportPath);
            }
            deleteDirRecursive(deleteDir);
        }
    }

    @Override
    public void importProgressPercentage(Report report, int percent) {
        report.setDescription(context.getString(R.string.import_pending) + "  " + percent + "%");
        broadcastUpdateReportList();
    }

    @Override
    public void importComplete(Report report) {
        ReportDescriptorUtil.readDescriptorAndUpdateReport(report);
        report.setEnabled(true);
        broadcastUpdateReportList();
    }

    @Override
    public void importError(Report report) {
        report.setEnabled(false);
        report.setDescription(context.getString(R.string.import_error));
        broadcastUpdateReportList();
    }

    private void findExistingReports() {
        // TODO: do on background thread?
        Log.i(TAG, "finding existing reports in dir " + reportsDir);
        File[] existingReports = reportsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                // TODO: check recognized file type
                return path.isFile() || path.isDirectory();
            }
        });
        for (File reportPath : existingReports) {
            Log.d(TAG, "found existing potential report: " + reportPath);
            Uri reportUri = Uri.fromFile(reportPath);
            if (uriCouldBeReport(reportUri) || pathIsHtmlContentRoot(reportPath)) {
                Report report = addNewReportForUri(Uri.fromFile(reportPath));
                if (reportPath.isFile()) {
                    new CheckReportSourceFileStability(report, reportPath).schedule();
                }
                else if (reportPath.isDirectory()) {
                    report.setPath(reportPath);
                    ReportDescriptorUtil.readDescriptorAndUpdateReport(report);
                    report.setEnabled(true);
                }
            }
        }
    }

    private boolean pathIsHtmlContentRoot(File path) {
        if (!path.isDirectory()) {
            return false;
        }
        File index = new File(path, "index.html");
        return index.exists();
    }

    private boolean uriCouldBeReport(Uri uri) {
        String ext = extensionOfFile(uri.getPath()).toLowerCase();
        if (supportedReportFileTypes.contains(ext)) {
            return true;
        }
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            return supportedReportFileTypes.contains(mimeType);
        }
        return false;
    }

    private String extensionOfFile(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot > path.length() - 2) {
            return "";
        }
        return path.substring(dot + 1);
    }

    private void broadcastUpdateReportList() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(INTENT_UPDATE_REPORT_LIST));
    }

    private Report addNewReportForUri(Uri reportUri) {
        String fileName = reportUri.toString();
        long fileSize = -1;
        if ("file".equals(reportUri.getScheme())) {
            File reportFile = new File(reportUri.getPath());
            fileName = reportFile.getName();
            fileSize = reportFile.length();
        }
        else if ("content".equals(reportUri.getScheme())) {
            // TODO: test what happens when ADD_CONTENT selects a file from the report dropbox
            Cursor reportInfo = context.getContentResolver().query(reportUri, null, null, null, null);
            int nameCol = reportInfo.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeCol = reportInfo.getColumnIndex(OpenableColumns.SIZE);
            reportInfo.moveToFirst();
            fileName = reportInfo.getString(nameCol);
            fileSize = reportInfo.getLong(sizeCol);
            reportInfo.close();
        }

        Report report = new Report();
        report.setSourceFile(reportUri);
        report.setSourceFileName(fileName);
        report.setSourceFileSize(fileSize);
        report.setTitle(fileName);
        report.setDescription(context.getString(R.string.import_pending));
        report.setEnabled(false);

        handler.post(new AddReportOnUIThread(report));

        return report;
    }

    /**
     * Continue the import process after the source file is stabilized by unzipping or
     * copying it to the reports directory.
     * @param report
     */
    private void continueImport(Report report) {
        String fileName = report.getSourceFileName();
        String extension = extensionOfFile(fileName);
        String mimeType = context.getContentResolver().getType(report.getSourceFile());

        if ("application/zip".equals(mimeType) || "zip".equalsIgnoreCase(extension)) {
            // TODO: delete zip if in dropbox
            String simpleName = fileName.substring(0, fileName.lastIndexOf("."));
            File unzipDir = new File(reportsDir, simpleName);
            report.setPath(unzipDir);
            new UnzipReportSourceFile(report, reportsDir, context, this).executeOnExecutor(importExecutor);
        }
        else {
            File destPath = new File(reportsDir, fileName);
            if (!destPath.exists()) {
                // file stability check should be handling this
                // TODO: unnecessary if dropbox dir and reports dir are separate
                report.setPath(destPath);
                new CopyReportSourceFileToReportPath(report).executeOnExecutor(importExecutor);
            }
        }
    }

    private Report getReportWithPath(File path) {
        for (Report r : reports) {
            if (r.getPath().equals(path)) {
                return r;
            }
        }
        return null;
    }

    private void fileArrivedInDropbox(File file) {
        // TODO: support copying directory?
        if (!file.isFile()) {
            return;
        }
        // TODO: unnecessary if the dropbox dir and reports dir are different
        Report report = getReportWithPath(file);
        if (report == null) {
            Uri uri = Uri.fromFile(file);
            if (uriCouldBeReport(uri)) {
                // not imported yet
                report = addNewReportForUri(Uri.fromFile(file));
                new CheckReportSourceFileStability(report, file).schedule();
            }
        }
    }

    /**
     * After a dropbox file has stabilized, finish importing the file.
     * @param report
     */
    private void sourceFileDidStabilize(Report report) {
        continueImport(report);
    }

    private void removeReportFromList(Report report) {
        handler.post(new RemoveReportOnUIThread(report));
    }

    private boolean deleteDirRecursive(File dir) {
        for (File child : dir.listFiles()) {
            if (child.isFile()) {
                if (!child.delete()) {
                    Log.e(TAG, "failed to delete file: " + child);
                    return false;
                }
            }
            else if (child.isDirectory()) {
                if (!deleteDirRecursive(child)) {
                    Log.e(TAG, "failed to delete directory recursively: " + child);
                    return false;
                }
            }
            if (!child.delete()) {
                Log.e(TAG, "failed to delete empty directory: " + child);
                return false;
            }
        }
        return true;
    }

	private class AddReportOnUIThread implements Runnable {
		private final Report report;
		private AddReportOnUIThread(Report report) {
			this.report = report;
		}
		@Override
		public void run() {
			reports.add(report);
			broadcastUpdateReportList();
		}
	}

    private class RemoveReportOnUIThread implements Runnable {
        private final Report report;
        private RemoveReportOnUIThread(Report report) {
            this.report = report;
        }
        @Override
        public void run() {
            if (reports.remove(report)) {
                broadcastUpdateReportList();
            }
        }
    }

    private class DropboxObserver extends FileObserver {

        public DropboxObserver() {
            super(dropboxDir.getAbsolutePath(), 0 |
                    FileObserver.CREATE | FileObserver.MOVED_TO |
                    FileObserver.DELETE | FileObserver.MOVED_FROM);
        }

        @Override
        public void onEvent(int event, String path) {
            // because http://stackoverflow.com/a/20609634/969164
            event &= FileObserver.ALL_EVENTS;
            Log.i(TAG, "file event: " + nameOfFileEvent(event) + "; " + path);
            File reportFile = new File(dropboxDir, path);
            if (event == FileObserver.DELETE || event == FileObserver.MOVED_FROM) {
                // TODO: something; nothing happens when the app is suspended
            }
            else if (event == FileObserver.CREATE || event == FileObserver.MOVED_TO) {
                fileArrivedInDropbox(reportFile);
            }
        }
    }

    /**
     * TODO: call {@link #importReportFromUri(android.net.Uri)} immediately when a report file is found and continue normal import after stable
     */
    private class CheckReportSourceFileStability implements Runnable {
        private final Report report;
        private final File sourceFile;
        private int stableCount = 0;
        private long lastModified = 0;
        private long lastLength = 0;
        private CheckReportSourceFileStability(Report report, File sourceFile) {
            this.report = report;
            this.sourceFile = sourceFile;
            lastLength = sourceFile.length();
            lastModified = sourceFile.lastModified();
        }
        private boolean fileIsStable() {
            return sourceFile.lastModified() == lastModified && sourceFile.length() == lastLength;
        }
        public void schedule() {
            scheduledExecutor.schedule(this, STABILITY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }
        @Override
        public void run() {
            // TODO: handle zero-length file gracefully
            if (fileIsStable()) {
                if (++stableCount >= MIN_STABILITY_CHECKS) {
                    report.setSourceFileSize(sourceFile.length());
                    sourceFileDidStabilize(report);
                }
                else {
                    schedule();
                }
            }
            else {
                stableCount = 0;
                lastLength = sourceFile.length();
                lastModified = sourceFile.lastModified();
                schedule();
            }
        }
    }

    /**
     * Copy a standalone report file (PDF, MS Word, etc.) to the reports directory.
     */
    private class CopyReportSourceFileToReportPath extends AsyncTask<Void, Void, Boolean> {

        private final Report report;

        private CopyReportSourceFileToReportPath(Report report) {
            this.report = report;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            FileDescriptor fd;
            try {
                fd = context.getContentResolver().openFileDescriptor(report.getSourceFile(), "r").getFileDescriptor();
            }
            catch (FileNotFoundException e) {
                // TODO: user feedback
                Log.e(TAG, "error opening file descriptor for report uri: " + report.getSourceFile(), e);
                return false;
            }
            FileChannel source = new FileInputStream(fd).getChannel();

            // TODO: handle existing file - ask user to overwrite or rename?
            File destFile = report.getPath();
            FileChannel dest;
            try {
                dest = new FileOutputStream(destFile).getChannel();
            }
            catch (FileNotFoundException e) {
                // TODO: user feedback
                Log.e(TAG, "error creating new report file for import: " + destFile, e);
                return false;
            }

            try {
                long sourceSize = source.size();
                long transferred = source.transferTo(0, sourceSize, dest);
                return transferred == sourceSize;
            }
            catch (IOException e) {
                Log.e(TAG, "error copying report file from " + report.getSourceFile() + " to " + destFile, e);
                return false;
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
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                report.setDescription("");
                importComplete(report);
            }
            else {
                importError(report);
            }
        }
    }

}
