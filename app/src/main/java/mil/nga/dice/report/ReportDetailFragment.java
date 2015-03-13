package mil.nga.dice.report;

import java.io.File;

import android.annotation.TargetApi;
import android.os.Build;

import mil.nga.dice.JavaScriptAPI;
import mil.nga.dice.R;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.fangjian.WebViewJavascriptBridge;

/**
 * A fragment representing a single Report detail screen. This fragment is
 * either contained in a {@link ReportDetailActivity}.
 */
public class ReportDetailFragment extends android.support.v4.app.Fragment {
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
		File reportFile = new File(mReport.getPath(), "index.html");
		if (reportFile.canRead() && reportFile.length() > 0) {
			WebView webView = (WebView) rootView.findViewById(R.id.report_detail);
            WebSettings settings = webView.getSettings();
            settings.setDomStorageEnabled(true);
            settings.setJavaScriptEnabled(true);
            enableFileAjax(webView);

            JavaScriptAPI jsAPI = new JavaScriptAPI(this.getActivity(), mReport, webView);
			webView.loadUrl("file://"+ reportFile.getPath());
		}
		return rootView;
	}


	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void enableFileAjax(WebView webView) {
		webView.getSettings().setAllowFileAccessFromFileURLs(true);
	}
}
