package mil.nga.dice;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import mil.nga.dice.gridview.ReportGridFragment;
import mil.nga.dice.listview.ReportListFragment;
import mil.nga.dice.map.ReportMapFragment;
import mil.nga.dice.cardview.CardViewFragment;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;

/**
 * <h3>TODO:</h3>
 * <ol>
 *   <li>add reports using {@link Intent#ACTION_PICK} and/or {@link Intent#ACTION_GET_CONTENT}</li>
 *   <li>add reports using <a href="http://developer.android.com/guide/topics/providers/document-provider.html">Storage Access Framework</a></li>
 * </ol>
 */
public class ReportCollectionActivity extends ActionBarActivity implements ReportCollectionCallbacks {
    public static final String TAG = "ReportCollection";


    private int currentViewId = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_collection);

        if (savedInstanceState == null) {
            showListView();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_report_collection, menu);
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();

        // TODO: could this possibly be a redundant call wrt onActivityResult(),
        // like if the OS happens to stop this activity while the user browses for content to add?
        handleIntentData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_content) {
            Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
            getContent.addCategory(Intent.CATEGORY_OPENABLE);
            getContent.setType("*/*");
            // TODO: test multiple
            getContent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            getContent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            getContent = Intent.createChooser(getContent, getString(R.string.title_add_content));
            startActivityForResult(getContent, 0);
        }
        if (id == R.id.action_about) {
            // TODO: add an about activity
            return true;
        }

        return showCollectionViewForOptionItemId(id);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        handleIntentData();
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
            File file = report.getPath();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }
    }

    private void handleIntentData() {
        Intent intent = getIntent();
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
            return false;
        }

        currentViewId = id;

        if (id == R.id.collection_view_list) {
            showListView();
        }
        else if (id == R.id.collection_view_grid) {
            showGridView();
        }
        else if (id == R.id.collection_view_map) {
            showMapView();
        }
        else if (id == R.id.collection_view_card) {
            showCardView();
        }

        return currentViewId == id;
    }

    private void showListView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new ReportListFragment())
                .commit();
    }

    private void showGridView() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new ReportGridFragment())
                .commit();
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
}
