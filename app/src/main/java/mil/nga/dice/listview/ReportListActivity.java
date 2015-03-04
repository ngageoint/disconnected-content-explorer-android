package mil.nga.dice.listview;

import java.util.ArrayList;

import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionCallbacks;
import mil.nga.dice.gridview.ReportGridActivity;
import mil.nga.dice.map.ReportMapActivity;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity representing a list of Reports. 
 * This activity implements the required
 * {@link ReportCollectionCallbacks} interface to listen for item selections.
 */
public class ReportListActivity extends Activity implements ReportCollectionCallbacks {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fragment_report_list);
	}

	
	/**
	 * Callback method from {@link ReportCollectionCallbacks} indicating that
	 * the given report was selected.
	 */
	@Override
	public void reportSelectedToView(Report report) {

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
