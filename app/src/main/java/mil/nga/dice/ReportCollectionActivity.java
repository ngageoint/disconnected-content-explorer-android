package mil.nga.dice;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import mil.nga.dice.about.DisclaimerDialogFragment;
import mil.nga.dice.about.LegalDetailsFragment;
import mil.nga.dice.map.ReportMapFragment;
import mil.nga.dice.cardview.CardViewFragment;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;

/**
 * <h3>TODO:</h3>
 * <ol>
 *   <li>fully test the activity life-cycle and the handling of the intent data for importing reports
 *     <ul>
 *       <li>proper life-cycle methods to implement</li>
 *       <li>when getIntent() returns the expected data</li>
 *       <li>leaving the activity with the home button</li>
 *       <li>leaving the activity by the add content action</li>
 *       <li>entering the activity from another app with the VIEW action</li>
 *       <li>more ... ?</li>
 *     </ul>
 *   </li>
 *     <li></li>
 *   <li>add reports using <a href="http://developer.android.com/guide/topics/providers/document-provider.html">Storage Access Framework</a></li>
 * </ol>
 */
public class ReportCollectionActivity extends ActionBarActivity
implements ReportCollectionCallbacks, DisclaimerDialogFragment.OnDisclaimerDialogDismissedListener, SwipeRefreshLayout.OnRefreshListener {
    
    public static final String TAG = "ReportCollection";
    
    public static final String HIDE_DISCLAIMER_KEY = "hide_disclaimer";

    private static Boolean showDisclaimer = null;

    private int currentViewId = 0;
    private boolean handlingAddContent = false;
    private SwipeRefreshLayout swipeToRefresh;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_collection);
        swipeToRefresh = (SwipeRefreshLayout) findViewById(R.id.report_collection_swipe_refresh);
        swipeToRefresh.setOnRefreshListener(this);
        swipeToRefresh.setEnabled(false);

        if (savedInstanceState == null) {
            showCardView();
        }

        if (showDisclaimer == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            showDisclaimer = preferences.getBoolean(HIDE_DISCLAIMER_KEY, true);
        }
        if (showDisclaimer) {
            showDisclaimer = false;
            DisclaimerDialogFragment dialogFragment = DisclaimerDialogFragment.newInstance();
            dialogFragment.show(getSupportFragmentManager(), "ReportCollectionActivity");
        }

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
        bm.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                swipeToRefresh.setRefreshing(false);
            }
        }, new IntentFilter(ReportManager.INTENT_END_REFRESH_REPORT_LIST));

        // let onActivityResult() do it
        if (!handlingAddContent) {
            handleIntentData(getIntent());
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        ReportManager.getInstance().refreshReports();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_report_collection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_content) {
            handlingAddContent = true;
            Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
            getContent.addCategory(Intent.CATEGORY_OPENABLE);
            getContent.setType("*/*");
            // TODO: test multiple
            getContent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            getContent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            getContent = Intent.createChooser(getContent, getString(R.string.title_add_content));
            startActivityForResult(getContent, 0);
            return true;
        }
        else if (id == R.id.action_refresh) {
            onRefresh();
            return true;
        }
        else if (id == R.id.action_about) {
            showAboutView();
            return true;
        }

        return showCollectionViewForOptionItemId(id);
    }

    @Override
    public void reportSelectedToView(Report report) {
        if (!report.isEnabled()) {
            return;
        }
        // TODO: figure out more robust file type handling - what does Android offer?
        // Start the detail activity for the selected report
        if (report.getFileExtension().equalsIgnoreCase("zip")) {
            Intent detailIntent = new Intent(this, ReportDetailActivity.class);
            detailIntent.putExtra("report", report);
            startActivity(detailIntent);
        }
        else if (report.getFileExtension().equalsIgnoreCase("pdf")) {
            // TODO: implement ContentProvider
            File file = report.getPath();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }
    }

    @Override
    public void onRefresh() {
        ReportManager.getInstance().refreshReports();
    }

    @Override
    public void onDisclaimerDialogDismissed(boolean exitApplication) {
        if (exitApplication) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        handleIntentData(data);
    }
    
    private void handleIntentData(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                if (clipData.getItemCount() > 0) {
                    ClipData.Item item = clipData.getItemAt(0);
                    uri = item.getUri();
                }
            }
        }
        if (uri == null) {
            return;
        }
        if ("dice".equals(uri.getScheme())) {
            // TODO: deep linking to specific report: navigateToReport(uri)
        }
        else {
            ReportManager.getInstance().importReportFromUri(uri);
        }
    }

    private void navigateToReport(Uri uri) {
        String srcScheme = uri.getQueryParameter("srcScheme");
        String reportId = uri.getQueryParameter("reportID");
        // TODO: ensure the report manager is bound first; test the callback sequence
        Report requestedReport = ReportManager.getInstance().getReportWithId(reportId);
        if (requestedReport != null) {
            Intent detailIntent = new Intent(this, ReportDetailActivity.class);
            detailIntent.putExtra("report", requestedReport);
            startActivity(detailIntent);
        }
    }

    private boolean showCollectionViewForOptionItemId(int id) {
        if (id == currentViewId) {
            return true;
        }

        currentViewId = id;

        if (id == R.id.collection_view_map) {
            showMapView();
        }
        else if (id == R.id.collection_view_card) {
            showCardView();
        }

        return currentViewId == id;
    }

    private void showMapView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new ReportMapFragment())
                .commit();
    }

    private void showCardView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new CardViewFragment())
                .commit();
    }

    private void showAboutView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new LegalDetailsFragment())
                .commit();
    }
}
