package mil.nga.dice.report;

import mil.nga.dice.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link ReportDetailFragment}.
 */
public class ReportDetailActivity extends ActionBarActivity {
	
	Report mReport;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report_detail);

		Bundle bundle = getIntent().getExtras();
		mReport = bundle.getParcelable("report");
		setTitle(mReport.getTitle());

		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			arguments.putString(ReportDetailFragment.ARG_ITEM_ID, getIntent().getStringExtra(ReportDetailFragment.ARG_ITEM_ID));
			arguments.putParcelable(ReportDetailFragment.ARG_REPORT, mReport);
			ReportDetailFragment fragment = new ReportDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.report_detail_container, fragment).commit();
		}
	}
	
	
	private void openNote() {
		Intent noteIntent = new Intent(this, NoteActivity.class);
		noteIntent.putExtra("report", mReport);
		startActivity(noteIntent);
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home: 
				finish();
				return true;
			case R.id.open_note:
				openNote();
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_report_detail, menu);
	    return true;
	}
}
