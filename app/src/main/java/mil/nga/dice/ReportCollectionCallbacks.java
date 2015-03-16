package mil.nga.dice;

import java.util.List;

import mil.nga.dice.report.Report;


public interface ReportCollectionCallbacks {
    List<Report> getReports();
    void reportSelectedToView(Report report);
}
