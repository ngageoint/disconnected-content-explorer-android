package mil.nga.dice.report;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
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
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.nga.dice.DICEConstants;
import mil.nga.dice.R;
import mil.nga.geopackage.GeoPackageConstants;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;

/**
 * TODO: modify to look for content roots in report dir instead of tying to source file
 */
public class ReportManager implements ReportImportCallbacks {

    public static final String INTENT_END_REFRESH_REPORT_LIST = "mil.nga.giat.dice.ReportManager.END_REFRESH_REPORT_LIST";
    public static final String INTENT_UPDATE_REPORT_LIST = "mil.nga.giat.dice.ReportManager.UPDATE_REPORT_LIST";
    public static final String USER_GUIDE_REPORT_ID = "mil.nga.giat.dice.downloadUserGuide";

    private static final String TAG = ReportManager.class.getSimpleName();

    private static final String DELETE_FILE_PREFIX = ".deleting.";

    private static final long STABILITY_CHECK_INTERVAL = 250;
    private static final int MIN_STABILITY_CHECKS = 2;

    private Report userGuideReport = new Report();

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

    private static void ensureUiThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new Error("not on ui thread!");
        }
    }

	private final List<Report> reports = new ArrayList<>();
	private final List<Report> reportsView = Collections.unmodifiableList(reports);

	private final Context context;
    private final File reportsDir;
    private final File notesDir;
    private final String externalContentThumbnail;
    private final Drawable externalContentIcon;
    private final Drawable thumbnailMissingIcon;
    private ScheduledExecutorService scheduledExecutor;
    private ExecutorService importExecutor;
	private Handler handler;

	public ReportManager(Context context) {
        super();

        if (instance != null) {
            throw new Error("too many ReportManager instances");
        }

        this.context = context.getApplicationContext();

        externalContentThumbnail = ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getApplicationInfo().packageName + "/" + String.valueOf(R.drawable.ic_launch);
        externalContentIcon = loadExternalLaunchIcon();

        thumbnailMissingIcon = loadThumbnailMissingIcon();

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
        int coreThreadCount = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        Log.d(TAG, "initializing import thread pool with " + coreThreadCount + " core threads");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreadCount, coreThreadCount,
                30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), backgroundThreads);
        executor.allowCoreThreadTimeOut(true);
        importExecutor = executor;

        handler = new Handler(Looper.getMainLooper());

        reportsDir = new File(Environment.getExternalStorageDirectory(), DICEConstants.DICE_REPORT_DIRECTORY);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        if (!reportsDir.isDirectory()) {
            throw new RuntimeException("content directory is not a directory or could not be created: " + reportsDir);
        }

        notesDir = new File(reportsDir, "notes");
        if (!notesDir.exists() && !notesDir.mkdirs()) {
            throw new RuntimeException("notes directory does not exist and could not be created: " + notesDir);
        }

        refreshReports();
	}

    public void destroy() {
        Log.i(TAG, "destruction");

        scheduledExecutor.shutdown();
        scheduledExecutor = null;
        importExecutor.shutdown();
        importExecutor = null;
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

    public void refreshReports() {
        removeDeletedAndErrorReports();
        findExistingReports();
        broadcastUpdateReportList();
        broadcastEndRefresh();
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

    public File getNotesDir() {
        return notesDir;
    }

    public File noteFileForReport(Report report) {
        return new File(notesDir, report.getTitle() + ".txt");
    }

    public Drawable thumbnailForReport(Report report) {
        if (report.getThumbnail() == null) {
            return thumbnailMissingIcon;
        }
        if (externalContentThumbnail.equals(report.getThumbnail())) {
            return externalContentIcon;
        }
        File imageFile = new File(report.getPath(), report.getThumbnail());
        if (imageFile.exists()) {
            return Drawable.createFromPath(imageFile.getAbsolutePath());
        }
        return thumbnailMissingIcon;
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

        if (reports.contains(userGuideReport)) {
            reports.remove(userGuideReport);
        }

        Report report = addNewReportForUri(reportUri);
        broadcastUpdateReportList();
        continueImport(report);
	}

    // TODO: do something with this
    public void deleteReport(Report report) {
        renameThenDeleteInBackground(report.getPath());
    }

    @Override
    public void importProgressPercentage(Report report, int percent) {
        report.setDescription(context.getString(R.string.import_pending) + "  " + percent + "%");
        broadcastUpdateReportList();
    }

    @Override
    public void importComplete(Report report) {
        ReportDescriptorUtil.readDescriptorAndUpdateReport(report);
        loadCacheFiles(report);
        deleteSourceFileIfInDropbox(report);
        removeDuplicatesOf(report);
        report.setEnabled(true);
        broadcastUpdateReportList();
    }

    @Override
    public void importError(Report report) {
        report.setEnabled(false);
        report.setDescription(context.getString(R.string.import_error));
        if (report.getError() == null) {
            report.setError(report.getDescription());
        }
        broadcastUpdateReportList();
    }

    private Drawable loadExternalLaunchIcon() {
        int color = context.getResources().getColor(R.color.colorPrimaryDark);
        int red = (color & 0x00ff0000) >> 16;
        int green = (color & 0x0000ff00) >> 8;
        int blue = color & 0x000000ff;
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(new float[]{
                0, 0, 0, 0, red,
                0, 0, 0, 0, green,
                0, 0, 0, 0, blue,
                0, 0, 0, 1, 0,
        });
        Drawable drawable = context.getResources().getDrawable(R.drawable.ic_launch);
        drawable.setColorFilter(filter);
        return drawable;
    }

    private Drawable loadThumbnailMissingIcon() {
        return context.getResources().getDrawable(R.drawable.thumbnail_missing);
    }

    /**
     * Call on main thread only
     */
    private void removeDeletedAndErrorReports() {
        ensureUiThread();

        Iterator<Report> reportIterator = reports.iterator();
        while (reportIterator.hasNext()) {
            Report report = reportIterator.next();
            if ((report.isEnabled() && report.getPath() != null && !report.getPath().exists()) ||
                    report.getError() != null) {
                reportIterator.remove();
            }
        }
    }

    /**
     * Call on main thread only.
     */
    private void findExistingReports() {
        ensureUiThread();

        Log.i(TAG, "finding existing reports in " + reportsDir);

        File[] existingReports = reportsDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                return !path.getName().startsWith(DELETE_FILE_PREFIX) &&
                        (uriCouldBeReport(Uri.fromFile(path)) || pathIsHtmlContentRoot(path));
            }
        });

        for (File reportPath : existingReports) {
            Log.d(TAG, "found existing potential report: " + reportPath);
            Report report = getReportWithPath(reportPath);
            if (report == null) {
                report = addNewReportForUri(Uri.fromFile(reportPath));
                if (reportPath.isFile()) {
                    new CheckReportSourceFileStability(report, reportPath).schedule();
                }
                else if (reportPath.isDirectory()) {
                    report.setPath(reportPath);
                    ReportDescriptorUtil.readDescriptorAndUpdateReport(report);
                    loadCacheFiles(report);
                    report.setEnabled(true);
                }
                else {
                    report.setError(context.getString(R.string.import_error_unsupported));
                    report.setDescription(report.getError());
                }
            }
        }

        // If there are no reports, point them towards to user guide
        if (reports.isEmpty()) {
            Log.d(TAG, "No reports, adding user guide placeholder");
            userGuideReport.setTitle("Tap here to download the user guide");
            userGuideReport.setDescription("After the download is complete, tap the notification and select \"Open in DICE\"");
            userGuideReport.setEnabled(true);
            userGuideReport.setId(USER_GUIDE_REPORT_ID);
            reports.add(userGuideReport);
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

    private void broadcastEndRefresh() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(INTENT_END_REFRESH_REPORT_LIST));
    }

    /**
     * Call on the main thread only.  Create a new {@link Report} object for the given source file URI and add it to the
     * {@link #reports list}.  The new Report object will be updated during and/or after the import process.
     *
     * @param sourceFile the {@link android.net.Uri} that points to the source file of the report
     * @return the new added {@link Report} object
     */
    private Report addNewReportForUri(Uri sourceFile) {
        ensureUiThread();

        Report report = getReportWithSourceFile(sourceFile);
        if (report != null) {
            // already got it - move on
            return report;
        }

        String fileName = sourceFile.toString();
        long fileSize = -1;
        if ("file".equals(sourceFile.getScheme())) {
            File reportFile = new File(sourceFile.getPath());
            report = getReportWithPath(reportFile);
            if (report != null) {
                // source file is probably a file in the reports dir
                return report;
            }
            fileName = reportFile.getName();
            fileSize = reportFile.length();
        }
        else if ("content".equals(sourceFile.getScheme())) {
            // TODO: test what happens when ADD_CONTENT selects a file from the report dropbox
            Cursor reportInfo = context.getContentResolver().query(sourceFile, null, null, null, null);
            int nameCol = reportInfo.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeCol = reportInfo.getColumnIndex(OpenableColumns.SIZE);
            reportInfo.moveToFirst();
            fileName = reportInfo.getString(nameCol);
            fileSize = reportInfo.getLong(sizeCol);
            reportInfo.close();
        }

        report = new Report();
        report.setSourceFile(sourceFile);
        report.setSourceFileName(fileName);
        report.setSourceFileSize(fileSize);
        report.setTitle(fileName);
        report.setDescription(context.getString(R.string.import_pending));
        report.setEnabled(false);

        reports.add(report);

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
            // file stability check should be handling this
            // TODO: unnecessary if dropbox dir and reports dir are separate
            report.setPath(destPath);
            report.setThumbnail(externalContentThumbnail);
            new CopyReportSourceFileToReportPath(report).executeOnExecutor(importExecutor);
        }
    }

    private void deleteSourceFileIfInDropbox(Report report) {
        if (!"file".equals(report.getSourceFile().getScheme())) {
            // only reports imported from the dropbox dir should have file:// uris
            return;
        }
        File sourceFile = new File(report.getSourceFile().getPath());
        if (reportsDir.equals(sourceFile.getParentFile()) && !sourceFile.equals(report.getPath()) /* when dropbox and reports dir are the same */ ) {
            renameThenDeleteInBackground(sourceFile);
        }
    }

    private void removeDuplicatesOf(Report report) {
        Iterator<Report> reportIterator = reports.iterator();
        while (reportIterator.hasNext()) {
            Report dup = reportIterator.next();
            if (dup != report && report.getPath().equals(dup.getPath())) {
                reportIterator.remove();
            }
        }
    }

    private Report getReportWithPath(File path) {
        for (Report r : reports) {
            if (path.equals(r.getPath())) {
                return r;
            }
        }
        return null;
    }

    private Report getReportWithSourceFile(Uri sourceFile) {
        for (Report r : reports) {
            if (sourceFile.equals(r.getSourceFile())) {
                return r;
            }
        }
        return null;
    }

    private void fileArrivedInDropbox(final File file) {
        // TODO: support copying directory?
        if (!file.isFile()) {
            return;
        }
        // TODO: unnecessary if the dropbox dir and reports dir are different
        handler.post(new Runnable() {
            @Override
            public void run() {
                Report report = getReportWithPath(file);
                if (report != null) {
                    return;
                }
                Uri uri = Uri.fromFile(file);
                if (uriCouldBeReport(uri)) {
                    // not imported yet
                    report = addNewReportForUri(Uri.fromFile(file));
                    broadcastUpdateReportList();
                    new CheckReportSourceFileStability(report, file).schedule();
                }
            }
        });
    }

    private void fileRemovedFromDropbox(final File file) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Report report = getReportWithPath(file);
                if (report == null) {
                    return;
                }
                reports.remove(report);
                broadcastUpdateReportList();
            }
        });
    }

    /**
     * After a dropbox file has stabilized, finish importing the file.
     * @param report
     */
    private void sourceFileDidStabilize(Report report) {
        continueImport(report);
    }

    private boolean renameThenDeleteInBackground(final File path) {
        File deletePath = new File(path.getParent(), DELETE_FILE_PREFIX + path.getName());
        if (!path.renameTo(deletePath)) {
            Log.e(TAG, "failed to rename path for deleting: " + path);
        }
        new DeleteRecursive(deletePath).executeOnExecutor(importExecutor);
        return true;
    }

    /**
     * This is a {@link java.lang.Runnable} that will continue to check whether a file
     * has changed every {@link #STABILITY_CHECK_INTERVAL} and reports when no changes
     * have occurred after {@link #MIN_STABILITY_CHECKS}.  This is intended to avoid
     * operating on files that are in the process of transferring from another device
     * or file system.
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
            if (report.getPath().exists()) {
                return true;
            }
            FileDescriptor fd;
            try {
                fd = context.getContentResolver().openFileDescriptor(report.getSourceFile(), "r").getFileDescriptor();
            }
            catch (FileNotFoundException e) {
                // TODO: user feedback
                Log.e(TAG, "error opening file descriptor for report uri: " + report.getSourceFile(), e);
                report.setError(e.getMessage());
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
                report.setError(e.getMessage());
                return false;
            }

            try {
                long sourceSize = source.size();
                long transferred = source.transferTo(0, sourceSize, dest);
                return transferred == sourceSize;
            }
            catch (IOException e) {
                Log.e(TAG, "error copying report file from " + report.getSourceFile() + " to " + destFile, e);
                report.setError(e.getMessage());
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
            if (report.getError() == null && success) {
                report.setDescription("");
                importComplete(report);
            }
            else {
                importError(report);
            }
        }
    }

    private class DeleteRecursive extends AsyncTask<Void, Void, Boolean> {

        private final File path;

        private DeleteRecursive(File path) {
            this.path = path;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return deleteRecursive(path);
        }

        private boolean deleteRecursive(File file) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    if (!deleteRecursive(child)) {
                        Log.e(TAG, "failed to delete directory recursively: " + file);
                        return false;
                    }
                }
            }
            if (!file.delete()) {
                Log.e(TAG, "failed to delete file: " + file);
                return false;
            }
            return true;
        }

    }

    private void loadCacheFiles(Report report){

        List<File> files = new ArrayList<>();
        File path = report.getPath();
        getCacheFiles(path, files);

        for(File file: files){

            String fileString = file.getAbsolutePath();
            String fileSubPath = fileString.replaceFirst(path.getAbsolutePath(), "");
            if(fileSubPath.startsWith(File.separator)){
                fileSubPath = fileSubPath.substring(1);
            }
            boolean shared = fileSubPath.startsWith(DICEConstants.DICE_REPORT_SHARED_DIRECTORY + File.separator);

            String nameWithExtension = file.getName();
            String name = removeExtension(nameWithExtension);

            String reportName = removeExtension(report.getId());
            name = reportIdPrefix(name, reportName, shared);
            if(shared){
                GeoPackageManager manager = GeoPackageFactory.getManager(context);
                if(!manager.exists(name)) {
                    manager.importGeoPackageAsExternalLink(file, name);
                }
            }

            ReportCache reportCache = new ReportCache(name, fileString, shared);
            report.addReportCache(reportCache);
        }
    }

    private String removeExtension(String name){
        String nameWithoutExtension = name;
        int i = name.lastIndexOf('.');
        if (i > 0) {
            nameWithoutExtension = name.substring(0, i);
        }
        return nameWithoutExtension;
    }

    private void getCacheFiles(File path, List<File> files){

        if(path.isDirectory()) {
            for (File file : path.listFiles()) {
                getCacheFiles(file, files);
            }
        }else{
            String stringPath = path.getAbsolutePath();
            if(stringPath.endsWith("." + GeoPackageConstants.GEOPACKAGE_EXTENSION)
                    || stringPath.endsWith("." + GeoPackageConstants.GEOPACKAGE_EXTENDED_EXTENSION)){
                files.add(path);
            }
        }
    }

    // TODO move?
    public static String reportIdPrefix(String report){
        String reportIdPrefix = report;
        if(reportIdPrefix != null){
            reportIdPrefix = DICEConstants.DICE_TEMP_CACHE_SUFFIX + reportIdPrefix + "-";
        }
        return reportIdPrefix;
    }

    // TODO move?
    public static String reportIdPrefix(String name, String report, boolean share){
        String reportId = name;
        if(!share){
            String reportIdPrefix = reportIdPrefix(report);
            if(reportIdPrefix != null){
                reportId = reportIdPrefix + reportId;
            }else{
                reportId = null;
            }
        }
        return reportId;
    }

}
