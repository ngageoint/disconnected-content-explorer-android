package mil.nga.dice.report;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import mil.nga.dice.R;

/**
 * WebView Javascript Bridge Client
 */
public class WebViewJavascriptBridgeClient extends WebViewClient {

    /**
     * Application context
     */
    protected final Context context;

    public WebViewJavascriptBridgeClient(Context context) {
        this.context = context;
    }

    @Override
    public void onPageFinished(WebView webView, String url) {
        Log.d("JSBridge", "onPageFinished");
        loadWebViewJavascriptBridgeJs(webView);
    }

    private void loadWebViewJavascriptBridgeJs(WebView webView) {
        InputStream is = context.getResources().openRawResource(R.raw.webviewjavascriptbridge);
        String script = convertStreamToString(is);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(script, null);
        }
        else {
            webView.loadUrl("javascript:" + script);
        }
    }

    public static String convertStreamToString(java.io.InputStream is) {
        String s = "";
        try {
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            if (scanner.hasNext()) s = scanner.next();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

}