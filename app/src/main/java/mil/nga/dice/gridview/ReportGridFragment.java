package mil.nga.dice.gridview;

import java.util.List;

import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;
import mil.nga.dice.R;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.AdapterView;

public class ReportGridFragment extends Fragment implements AdapterView.OnItemClickListener {
	
	private List<Report> mReports;
	private CustomGrid mReportsAdapter;
	ReportManager mReportManager;
	
	public static final String ARG_REPORTS = "reports";
	private static final String TAG = "ReportGridFragment";
	
	
	public ReportGridFragment() {}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mReportManager = ReportManager.getInstance();
		mReports = mReportManager.getReports();
		mReportsAdapter = new CustomGrid(getActivity(), mReports);
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.activity_report_grid, container, false);
		final GridView gridView = (GridView) v.findViewById(R.id.report_grid);
		gridView.setAdapter(mReportsAdapter);
		gridView.setOnItemClickListener(this);
		return v;
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.i(TAG, "Grid item clicked at position " + position);
		
		Report report = mReports.get(position);
		if (report.isEnabled()) {
			Intent detailIntent = new Intent(getActivity(), ReportDetailActivity.class);
			detailIntent.putExtra("report", report);
			startActivity(detailIntent);
		}
	}
}
