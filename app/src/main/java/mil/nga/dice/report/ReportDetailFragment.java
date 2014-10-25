package mil.nga.dice.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import mil.nga.dice.R;
import mil.nga.dice.R.id;
import mil.nga.dice.R.layout;
import mil.nga.dice.listview.ReportListActivity;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * A fragment representing a single Report detail screen. This fragment is
 * either contained in a {@link ReportDetailActivity}.
 */
public class ReportDetailFragment extends Fragment {
	/**
	 * The fragment argument representing the item ID that this fragment
	 * represents.
	 */
	public static final String ARG_ITEM_ID = "item_id";
	public static final String ARG_REPORT = "report";
	private Report mReport;

	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ReportDetailFragment() {
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (getArguments().containsKey(ARG_REPORT)) {
			mReport = getArguments().getParcelable(ARG_REPORT);
		}
	}

	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_report_detail, container, false);
		if (mReport == null) {
			return rootView;
		}
		File reportFile = new File(mReport.getPath() + "/index.html");
		if (reportFile.canRead() && reportFile.length() > 0) {
			WebView webView = (WebView) rootView.findViewById(R.id.report_detail);
			webView.getSettings().setJavaScriptEnabled(true);
			webView.loadUrl(reportFile.toURI().toASCIIString());
		}
		return rootView;
	}

}