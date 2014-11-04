package mil.nga.dice.gridview;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import mil.nga.dice.R;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;

public class ReportGridFragment extends Fragment implements AdapterView.OnItemClickListener {

	public static final String ARG_REPORTS = "reports";

	private CustomGrid mReportsAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mReportsAdapter = new CustomGrid(getActivity(), ReportManager.getInstance().getReports());
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity());
		bm.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mReportsAdapter.notifyDataSetChanged();
			}
		}, new IntentFilter(ReportManager.INTENT_UPDATE_REPORT_LIST));
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
		Report report = (Report) mReportsAdapter.getItem(position);
		if (report.isEnabled()) {
			Intent detailIntent = new Intent(getActivity(), ReportDetailActivity.class);
			detailIntent.putExtra("report", report);
			startActivity(detailIntent);
		}
	}
}
