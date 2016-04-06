package mil.nga.dice;

import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;

import mil.nga.dice.about.AboutActivity;
import mil.nga.dice.about.DisclaimerDialogFragment;
import mil.nga.dice.cardview.CardViewFragment;
import mil.nga.dice.io.DICEFileUtils;
import mil.nga.dice.map.ReportMapFragment;
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
    
    private static final String PREF_SHOW_DISCLAIMER = "show_disclaimer";

    /**
     * Permissions request code for importing a GeoPackage as an external link
     */
    public static final int PERMISSIONS_REQUEST_IMPORT_GEOPACKAGE = 200;

    /**
     * Intent activity request code when opening app settings
     */
    public static final int ACTIVITY_APP_SETTINGS = 3344;

    public static final int OVERLAYS_ACTIVITY = 100;

    private static Boolean showDisclaimer = null;

    private int currentViewId = 0;
    private boolean handlingAddContent = false;

    /**
     * GeoPackage cache for importing GeoPackage files used to open DICE
     */
    private GeoPackageCache geoPackageCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report_collection);

        geoPackageCache = new GeoPackageCache(this);

        if (showDisclaimer == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            showDisclaimer = preferences.getBoolean(PREF_SHOW_DISCLAIMER, true);
        }
        if (showDisclaimer) {
            showDisclaimer = false; // don't show it again while the process lives
            DisclaimerDialogFragment dialogFragment = new DisclaimerDialogFragment();
            dialogFragment.setCancelable(false);
            dialogFragment.show(getSupportFragmentManager(), "ReportCollectionActivity");
        }

        if (savedInstanceState == null) {
            showCardView();
        }
        if (savedInstanceState == null && !handlingAddContent) {
            handleIntentData(getIntent());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
            getContent = Intent.createChooser(getContent, getString(R.string.action_add_content));
            startActivityForResult(getContent, 0);
            return true;
        }
        else if (id == R.id.action_refresh) {
            onRefresh();
            return true;
        }
        else if (id == R.id.action_about) {
            startAboutActivity();
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
        if (report.getPath().isDirectory()) {
            Intent detailIntent = new Intent(this, ReportDetailActivity.class);
            detailIntent.putExtra("report", report);
            startActivity(detailIntent);
        }
        else {
            File reportPath = report.getPath();
            Uri reportUri = Uri.fromFile(reportPath);
            String contentType = getContentResolver().getType(reportUri);
            if (contentType == null) {
                String ext = MimeTypeMap.getFileExtensionFromUrl(reportUri.getEncodedPath());
                if (ext != null) {
                    contentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                }
            }
            Intent viewContent = new Intent(Intent.ACTION_VIEW);
            viewContent.setDataAndType(reportUri, contentType);
            viewContent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            if (viewContent.resolveActivity(getPackageManager()) != null) {
                startActivity(viewContent);
            }
            else {
                Toast.makeText(this, R.string.no_viewer_for_report, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRefresh() {
        ReportManager.getInstance().refreshReports();
    }

    @Override
    public void onDisclaimerDialogAgree(DisclaimerDialogFragment disclaimerDialog) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        boolean show = disclaimerDialog.isShowDisclaimerChecked();
        editor.putBoolean(PREF_SHOW_DISCLAIMER, show);
        editor.commit();
    }

    @Override
    public void onDisclaimerDialogDisagree(DisclaimerDialogFragment disclaimerDialog) {
        showDisclaimer = true;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(PREF_SHOW_DISCLAIMER);
        editor.commit();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handled = true;

        switch (requestCode) {

            case ACTIVITY_APP_SETTINGS:
                break;

            case OVERLAYS_ACTIVITY:
                // TODO
                break;

            default:
                handled = false;
        }

        if (!handled) {
            if (resultCode != RESULT_OK) {
                return;
            }

            handleIntentData(data);
        }

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
            // Attempt to get a file path and display name
            String path = FileUtils.getPath(this, uri);
            String name = DICEFileUtils.getDisplayName(this, uri, path);

            // If a GeoPackage file
            if(geoPackageCache.hasGeoPackageExtension(name)){
                geoPackageCache.importFile(name, uri, path);
            }else{
                // Attempt to import a report
                ReportManager.getInstance().importReportFromUri(uri);
            }
        }
    }

    private void navigateToReport(Uri uri) {
        String srcScheme = uri.getQueryParameter("srcScheme");
        String reportId = uri.getQueryParameter("reportID");
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

        if (currentViewId == R.id.action_about) {
            getSupportFragmentManager().popBackStackImmediate();
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

    private void startAboutActivity() {
        startActivity(new Intent(this, AboutActivity.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        // Check if permission was granted
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        switch(requestCode) {

            case PERMISSIONS_REQUEST_IMPORT_GEOPACKAGE:
                geoPackageCache.importGeoPackageExternalLinkAfterPermissionGranted(granted);
                break;

        }
    }

}
