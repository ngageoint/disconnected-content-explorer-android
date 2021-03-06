package mil.nga.dice.report;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.File;

import mil.nga.dice.JavaScriptAPI;
import mil.nga.dice.R;


public class ReportDetailActivity extends AppCompatActivity {

    /**
     * Location services request code
     */
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;

	Report mReport;
    WebView reportWebView;
    JavaScriptAPI jsApi;
    private GeoPackageWebViewClient geoPackageWebViewClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_report_detail);

		Bundle bundle = getIntent().getExtras();
		mReport = bundle.getParcelable("report");
		setTitle(mReport.getTitle());

        reportWebView = (WebView) findViewById(R.id.report_detail);
        WebSettings settings = reportWebView.getSettings();
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptEnabled(true);
        enableFileAjax(reportWebView);

        geoPackageWebViewClient = new GeoPackageWebViewClient(this, mReport.getId());

        File reportFile = new File(mReport.getPath(), "index.html");
        if (reportFile.canRead() && reportFile.length() > 0) {
            jsApi = JavaScriptAPI.addTo(reportWebView, mReport, this, geoPackageWebViewClient);
            if (savedInstanceState == null) {
                reportWebView.loadUrl(Uri.fromFile(reportFile).toString());
            }
            else {
                reportWebView.restoreState(savedInstanceState);
            }
        }

	}

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
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

    @Override
    public boolean onNavigateUp() {
        if (reportWebView.canGoBack()) {
            onBackPressed();
            return false;
        }
        return super.onNavigateUp();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (reportWebView.canGoBack()) {
            onBackPressed();
            return false;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        reportWebView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (reportWebView.canGoBack()) {
            reportWebView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(jsApi != null) {
            jsApi.removeFromWebView();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void enableFileAjax(WebView webView) {
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
    }

    private void openNote() {
        Intent noteIntent = new Intent(this, NoteActivity.class);
        noteIntent.putExtra("report", mReport);
        startActivity(noteIntent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        // Check if permission was granted
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        switch(requestCode) {

            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                jsApi.geolocateWithPermissions(granted);
                break;

        }
    }
}
