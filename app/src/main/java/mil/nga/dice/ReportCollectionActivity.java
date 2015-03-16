package mil.nga.dice;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import mil.nga.dice.gridview.ReportGridFragment;
import mil.nga.dice.listview.ReportListFragment;
import mil.nga.dice.map.ReportMapFragment;
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
public class ReportCollectionActivity extends Activity implements ReportCollectionCallbacks, ReportManager.ReportManagerClient {

    public static final String TAG = "ReportCollection";


    private ReportManager.Connection reportManagerConnection;
    private int currentViewId = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ReportManager.bindTo(this)) {
            throw new Error("failed to bind to ReportManager service");
        }

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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_content) {
            Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
            getContent.addCategory(Intent.CATEGORY_OPENABLE);
            getContent.setType("*/*");
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
        unbindService(reportManagerConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        handleIntentData();
    }

    @Override
    public void reportManagerConnected(ReportManager.Connection x) {
        reportManagerConnection = x;
        Fragment collectionView = getFragmentManager().findFragmentById(R.id.report_collection);
        if (collectionView instanceof ReportManager.ReportManagerClient) {
            ((ReportManager.ReportManagerClient) collectionView).reportManagerConnected(x);
        }
        // now that the report manager is ready, handle any potential data the activity starter is passing
        handleIntentData();
    }

    @Override
    public void reportManagerDisconnected() {
        reportManagerConnection = null;
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

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof ReportManager.ReportManagerClient && reportManagerConnection != null) {
            ((ReportManager.ReportManagerClient) fragment).reportManagerConnected(reportManagerConnection);
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
            importReportFromUri(uri);
        }
    }

    private void navigateToReport(Uri uri) {
        String srcScheme = uri.getQueryParameter("srcScheme");
        String reportId = uri.getQueryParameter("reportID");
        // TODO: ensure the report manager is bound first; test the callback sequence
        Report requestedReport = reportManagerConnection.getReportManager().getReportWithId(reportId);
        if (requestedReport != null) {
            Intent detailIntent = new Intent(this, ReportDetailActivity.class);
            detailIntent.putExtra("report", requestedReport);
            startActivity(detailIntent);
        }
    }

    private void importReportFromUri(Uri uri) {
        reportManagerConnection.getReportManager().importReportFromUri(uri);
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

        return currentViewId == id;
    }

    private void showListView() {
        getFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new ReportListFragment())
                .commit();
    }

    private void showGridView() {
        getFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new ReportGridFragment())
                .commit();
    }

    private void showMapView() {
        getFragmentManager().beginTransaction()
                .replace(R.id.report_collection, new ReportMapFragment())
                .commit();
    }
}
