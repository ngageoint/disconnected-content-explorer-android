package mil.nga.dice.about;

import android.os.Bundle;
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
        aboutWebView.loadUrl("file:///android_asset/legal/legal.html");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
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
