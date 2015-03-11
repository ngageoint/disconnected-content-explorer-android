package mil.nga.dice;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;

import mil.nga.dice.gridview.ReportGridFragment;
import mil.nga.dice.listview.ReportListFragment;
import mil.nga.dice.map.ReportMapFragment;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;

public class ReportCollectionActivity extends Activity implements ReportCollectionCallbacks {

    private int currentViewId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_collection);

        if (savedInstanceState == null) {
            showListView();
        }

        // TODO: If exposing deep links into your app, handle intents here.
        /*
         TODO: is this the right place for this?  what if dice was already
         in a different activity so this is not called, or launches to a
         different activity?  should this go in the DICE class?
        */
        Uri deepLinkUrl = getIntent().getData();
        if (deepLinkUrl != null) {
            Log.i("ReportCollection", "data url: " + deepLinkUrl);
            String srcScheme = deepLinkUrl.getQueryParameter("srcScheme");
            String reportId = deepLinkUrl.getQueryParameter("reportID");
            Report requestedReport = ReportManager.getInstance().getReportWithID(reportId);
            if (requestedReport != null) {
                Intent detailIntent = new Intent(this, ReportDetailActivity.class);
                detailIntent.putExtra("report", requestedReport);
                startActivity(detailIntent);
            }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            // TODO: add an about activity
            return true;
        }

        return showCollectionViewForOptionItemId(id);
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
}
