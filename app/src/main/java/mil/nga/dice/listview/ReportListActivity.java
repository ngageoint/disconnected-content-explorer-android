package mil.nga.dice.listview;

import java.io.File;
import java.util.ArrayList;

import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionCallbacks;
import mil.nga.dice.gridview.ReportGridActivity;
import mil.nga.dice.map.ReportMapActivity;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity representing a list of Reports. 
 * This activity implements the required
 * {@link ReportCollectionCallbacks} interface to listen for item selections.
 */
public class ReportListActivity extends Activity implements ReportCollectionCallbacks {
	
	String mSrcScheme;
	String mReportId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_report_list);
		
		// TODO: If exposing deep links into your app, handle intents here.
		Uri deepLinkUrl = getIntent().getData();
		if (deepLinkUrl != null) {
			mSrcScheme = deepLinkUrl.getQueryParameter("srcScheme");
			mReportId = deepLinkUrl.getQueryParameter("reportID");
			Log.i("ReportListActivity", "Params from URL: srcScheme " + mSrcScheme + " reportID " + mReportId);
			Report requestedReport = ReportManager.getInstance().getReportWithID(mReportId);
			if (requestedReport != null) {
				Intent detailIntent = new Intent(this, ReportDetailActivity.class);
				detailIntent.putExtra("report", requestedReport);
				startActivity(detailIntent);
			}
		}
	}

	
	/**
	 * Callback method from {@link ReportCollectionCallbacks} indicating that
	 * the given report was selected.
	 */
	@Override
	public void reportSelectedToView(Report report) {
		if (!report.isEnabled()) {
			return;
		}
		// Start the detail activity for the selected report
		if (report.getFileExtension().equalsIgnoreCase("zip")) {
			Intent detailIntent = new Intent(this, ReportDetailActivity.class);
			detailIntent.putExtra("report", report);
			startActivity(detailIntent);
		}
		else if (report.getFileExtension().equalsIgnoreCase("pdf")) {
			File file = report.getPath();
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(file), "application/pdf");
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			startActivity(intent);
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.list_menu, menu);
	    return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.tiles_button:
				showGridView();
				return true;
			case R.id.map_button:
				showMapView();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	private void showGridView() {
		Intent gridIntent = new Intent(this, ReportGridActivity.class);
		ReportManager reportManager = ReportManager.getInstance();
		ArrayList<Report> reportArrayList = new ArrayList<Report>(reportManager.getReports());
		gridIntent.putParcelableArrayListExtra("reports", reportArrayList);
		startActivity(gridIntent);
	}
	
	
	private void showMapView() {
		Intent mapIntent = new Intent(this, ReportMapActivity.class);
		ReportManager reportManager = ReportManager.getInstance();
		ArrayList<Report> reportArrayList = new ArrayList<Report>(reportManager.getReports());
		mapIntent.putParcelableArrayListExtra("reports", reportArrayList);
		startActivity(mapIntent);
	}
}
