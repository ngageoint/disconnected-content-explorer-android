package mil.nga.dice;

import android.app.Activity;
import android.app.Dialog;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.fangjian.WebViewJavascriptBridge;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;

/**

 */
public class JavaScriptAPI implements ConnectionCallbacks, OnConnectionFailedListener {
    private static final String TAG = "JavaScriptAPI";

    public static JavaScriptAPI addTo(WebView webView, Report report, Activity context) {
        return new JavaScriptAPI(context, report, webView);
    }

    private Activity mActivity;
    private Report mReport;
    private WebView mWebView;
    private File exportDirectory = new File(ReportManager.getInstance().getReportsDir(), "export");
    private WebViewJavascriptBridge bridge;
    private GoogleApiClient mGoogleApiClient;


    private JavaScriptAPI(Activity a, Report r, WebView w) {
        mActivity = a;
        mReport = r;
        mWebView = w;

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        Log.i(TAG, "Configuring JavascriptBridge");
        bridge = new WebViewJavascriptBridge(mActivity, mWebView, new UserServerHandler());

        bridge.registerHandler("getLocation", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i(TAG, "Bridge received a call to getLocation");
                if (jsCallback != null) {
                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location != null) {
                        // get the users location and send it back
                        jsCallback.callback("{\"success\":true,\"lat\":\"" + location.getLatitude() + "\",\"lon\":\"" + location.getLongitude() + "\"}");
                    }
                    else {
                        jsCallback.callback("{\"success\":false,\"message\":\"DICE could not determine your location.  Ensure location services are enabled on your device.\"}");
                    }
                }
            }
        });


        bridge.registerHandler("saveToFile", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                Log.i(TAG, "Bridge received a call to export data");
                if (jsCallback != null) {
                    File export = new File(exportDirectory, mReport.getTitle() + "_export.json");

                    if (!exportDirectory.exists()) {
                        exportDirectory.mkdir();
                    }

                    try {
                        export.createNewFile();
                        FileOutputStream fOut = new FileOutputStream(export);
                        fOut.write(data.getBytes());
                        fOut.flush();
                        fOut.close();
                        jsCallback.callback("{\"success\":true,\"message\":\"Exported your data to the DICE folder on your SD card.\"}");
                    } catch (IOException e) {
                        e.printStackTrace();
                        jsCallback.callback("{\"success\":false,\"message\":\"There was a problem exporting your data, please try again.\"}");
                        // TODO: send an error object back through the webview so the user can handle it
                    }
                }
            }
        });
    }

    public void removeFromWebView() {
        mGoogleApiClient.disconnect();
        mGoogleApiClient = null;
        mWebView = null;
        bridge = null;
    }


    class UserServerHandler implements WebViewJavascriptBridge.WVJBHandler {
        @Override
        public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
            Log.i(TAG, "DICE Android received a message from Javascript");
            if (jsCallback != null) {
                jsCallback.callback("Response from DICE");
            }
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mActivity.getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    @Override
    public void onConnected(Bundle bundle) {
    }


    @Override
    public void onConnectionSuspended(int i) {
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
