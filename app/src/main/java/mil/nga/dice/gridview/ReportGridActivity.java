package mil.nga.dice.gridview;

import java.util.ArrayList;

import mil.nga.dice.R;
import mil.nga.dice.R.id;
import mil.nga.dice.R.layout;
import mil.nga.dice.R.menu;
import mil.nga.dice.listview.ReportListActivity;
import mil.nga.dice.map.ReportMapActivity;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ReportGridActivity extends Activity {

	private static final String TAG = "ReportGridActivity";
	ArrayList<Report> mReports;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report_grid);
		
		Bundle bundle = getIntent().getExtras();
		mReports = bundle.getParcelableArrayList(ReportGridFragment.ARG_REPORTS);
		
		if (getFragmentManager().findFragmentByTag(TAG) == null) {
			final FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(android.R.id.content, new ReportGridFragment(), TAG);
			ft.commit();
		}
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.grid_menu, menu);
	    return true;
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.list_button:
				showListView();
				return true;
			case R.id.map_button:
				showMapView();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	private void showListView() {
		Intent listIntent = new Intent(this, ReportListActivity.class);
		ReportManager reportManger = ReportManager.getInstance();
		ArrayList<Report> reportArrayList = new ArrayList<Report>(reportManger.getReports());
		listIntent.putParcelableArrayListExtra("reports", reportArrayList);
		startActivity(listIntent);
	}
	
	
	private void showMapView() {
		Intent mapIntent = new Intent(this, ReportMapActivity.class);
		ReportManager reportManger = ReportManager.getInstance();
		ArrayList<Report> reportArrayList = new ArrayList<Report>(reportManger.getReports());
		mapIntent.putParcelableArrayListExtra("reports", reportArrayList);
		startActivity(mapIntent);
	}
}
