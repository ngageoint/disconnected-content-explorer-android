package mil.nga.dice.report;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import mil.nga.dice.DICEConstants;

/**
 * Report utilities
 */
public class ReportUtils {

    public static final String DELETE_FILE_PREFIX = ".deleting.";

    private static final Set<String> supportedReportFileTypes;
    static {
        Set<String> types = new TreeSet<>(Arrays.asList(new String[]{
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

    /**
     * Get the base report directory
     *
     * @return report directories
     */
    public static File getReportDirectory() {
        return new File(Environment.getExternalStorageDirectory(), DICEConstants.DICE_REPORT_DIRECTORY);
    }

    /**
     * Get the report directories
     *
     * @return report directories
     */
    public static File[] getReportDirectories(final Context context) {
        File reportDirectory = getReportDirectory();
        File[] reportDirectories = getReportDirectories(context, reportDirectory);
        return reportDirectories;
    }

    /**
     * Get the report directories from the provided base report directory
     *
     * @param reportDirectory
     * @return report directories
     */
    public static File[] getReportDirectories(final Context context, File reportDirectory) {
        File[] existingReports = reportDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                return !path.getName().startsWith(DELETE_FILE_PREFIX) &&
                        (uriCouldBeReport(context, Uri.fromFile(path)) || pathIsHtmlContentRoot(path));
            }
        });
        return existingReports;
    }

    private static boolean pathIsHtmlContentRoot(File path) {
        if (!path.isDirectory()) {
            return false;
        }
        File index = new File(path, "index.html");
        return index.exists();
    }

    public static boolean uriCouldBeReport(Context context, Uri uri) {
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

    /**
     * Get the extension of the file
     * @param path path
     * @return extension
     */
    public static String extensionOfFile(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot > path.length() - 2) {
            return "";
        }
        return path.substring(dot + 1);
    }

    /**
     * Get the local report path
     * @param path full path
     * @return local path
     */
    public static String localReportPath(File path){

        String reportFile = getReportDirectory().getAbsolutePath() + File.separator;
        String localPath = path.getAbsolutePath();
        if(localPath.startsWith(reportFile)){
            localPath = localPath.substring(reportFile.length());
        }
        return localPath;
    }

}
