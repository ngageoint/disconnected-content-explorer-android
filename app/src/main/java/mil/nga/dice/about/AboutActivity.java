package mil.nga.dice.about;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import mil.nga.dice.R;


public class AboutActivity extends ActionBarActivity {

    private WebView aboutWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        aboutWebView = (WebView) findViewById(R.id.about_web_view);
        aboutWebView.getSettings().setJavaScriptEnabled(true);

        if (savedInstanceState == null) {
            aboutWebView.loadUrl("file:///android_asset/legal/legal.html");
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
