package mil.nga.dice.about;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.webkit.WebView;

import mil.nga.dice.R;


public class AboutActivity extends AppCompatActivity {

    private WebView aboutWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        aboutWebView = (WebView) findViewById(R.id.about_web_view);
        aboutWebView.getSettings().setJavaScriptEnabled(true);

        if (savedInstanceState == null) {
            String aboutUrl = "file:///android_asset/legal/legal.html";
            try {
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = info.versionName;
                aboutUrl += "?version=" + version;
            }
            catch (PackageManager.NameNotFoundException e) {
                // hmmm
                e.printStackTrace();
            }
            aboutWebView.loadUrl(aboutUrl);
        }
        else {
            aboutWebView.restoreState(savedInstanceState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onNavigateUp() {
        if (aboutWebView.canGoBack()) {
            aboutWebView.goBack();
            return false;
        }
        return super.onNavigateUp();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (aboutWebView.canGoBack()) {
            onBackPressed();
            return false;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        aboutWebView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (aboutWebView.canGoBack()) {
            aboutWebView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }
}
