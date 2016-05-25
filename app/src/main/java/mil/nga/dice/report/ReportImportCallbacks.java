package mil.nga.dice.report;

import android.app.Activity;
import android.net.Uri;

/**
 * Created by stjohnr on 3/18/15.
 */
public interface ReportImportCallbacks {
    void importProgressPercentage(Report report, int value);
    void importComplete(Report report);
    void importError(Report report);
    void downloadProgressPercentage(Report report, int value);
    void downloadComplete(Report report, Activity activity, Uri reportZipPath);
    void downloadError(Report report, String errorMessage);
}
