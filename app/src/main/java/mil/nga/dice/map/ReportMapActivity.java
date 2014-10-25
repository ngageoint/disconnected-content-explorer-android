package mil.nga.dice.map;

import java.util.ArrayList;

import mil.nga.dice.R;
import mil.nga.dice.gridview.ReportGridActivity;
import mil.nga.dice.listview.ReportListActivity;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ReportMapActivity extends Activity {

	private static final String TAG = "ReportMapActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report_map);
		
		if (getFragmentManager().findFragmentByTag(TAG) == null) {
			final FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(android.R.id.content, new ReportMapFragment(), TAG);
			ft.commit();
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.map_menu, menu);
	    return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.tiles_button:
				showGridView();
				return true;
			case R.id.list_button:
				showListView();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	private void showGridView() {
		Intent gridIntent = new Intent(this, ReportGridActivity.class);
		ReportManager reportManger = ReportManager.getInstance();
		ArrayList<Report> reportArrayList = new ArrayList<Report>(reportManger.getReports());
		gridIntent.putParcelableArrayListExtra("reports", reportArrayList);
		startActivity(gridIntent);
	}
	
	
	private void showListView() {
		Intent listIntent = new Intent(this, ReportListActivity.class);
		ReportManager reportManger = ReportManager.getInstance();
		ArrayList<Report> reportArrayList = new ArrayList<Report>(reportManger.getReports());
		listIntent.putParcelableArrayListExtra("reports", reportArrayList);
		startActivity(listIntent);
	}
}
