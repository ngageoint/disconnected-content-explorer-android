package mil.nga.dice.report;

/**
 * Created by stjohnr on 3/18/15.
 */
public interface ReportImportCallbacks {
    void percentageComplete(Report report, int value);
    void importComplete(Report report);
    void importError(Report report);
}
