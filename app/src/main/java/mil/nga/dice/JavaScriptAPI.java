package mil.nga.dice;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.WebView;

import com.fangjian.WebViewJavascriptBridge;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import mil.nga.dice.report.GeoPackageWebViewClient;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;
import mil.nga.geopackage.BoundingBox;

/**

 */
public class JavaScriptAPI implements ConnectionCallbacks, OnConnectionFailedListener {
    private static final String TAG = "JavaScriptAPI";

    public static JavaScriptAPI addTo(WebView webView, Report report, Activity context, GeoPackageWebViewClient geoPackageClient) {
        return new JavaScriptAPI(context, report, webView, geoPackageClient);
    }

    private Activity mActivity;
    private Report mReport;
    private WebView mWebView;
    private File exportDirectory = new File(ReportManager.getInstance().getReportsDir(), "export");
    private WebViewJavascriptBridge bridge;
    private GoogleApiClient mGoogleApiClient;
    private GeoPackageWebViewClient geoPackage;
    private WebViewJavascriptBridge.WVJBResponseCallback jsCallbackAfterPermissions;

    private JavaScriptAPI(Activity a, Report r, WebView w, GeoPackageWebViewClient geoPackageClient) {
        mActivity = a;
        mReport = r;
        mWebView = w;
        geoPackage = geoPackageClient;

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        Log.i(TAG, "Configuring JavascriptBridge");
        bridge = new WebViewJavascriptBridge(mActivity, mWebView, new UserServerHandler(), geoPackage);

        bridge.registerHandler("getLocation", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                geolocate(jsCallback);
            }
        });

        bridge.registerHandler("saveToFile", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                exportJSON(data, jsCallback);
            }
        });

        bridge.registerHandler("click", new WebViewJavascriptBridge.WVJBHandler() {
            @Override
            public void handle(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback) {
                click(data, jsCallback);
            }
        });
    }

    private void exportJSON(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback){
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

                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("text/plain");
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mReport.getTitle() + " export");
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(export));
                mActivity.startActivity(Intent.createChooser(emailIntent, "Email your export"));
            } catch (IOException e) {
                e.printStackTrace();
                jsCallback.callback("{\"success\":false,\"message\":\"There was a problem exporting your data, please try again.\"}");
                // TODO: send an error object back through the webview so the user can handle it
            }
        }
    }

    private void geolocate(WebViewJavascriptBridge.WVJBResponseCallback jsCallback){
        Log.i(TAG, "Bridge received a call to getLocation");
        if (jsCallback != null) {
            Location location = null;

            jsCallbackAfterPermissions = jsCallback;
            if (ContextCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                geolocateWithPermissions(true);
            }else{
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(mActivity, R.style.AppCompatAlertDialogStyle)
                            .setTitle(R.string.location_access_rational_title)
                            .setMessage(R.string.location_access_rational_message)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ReportDetailActivity.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                                }
                            })
                            .create()
                            .show();

                } else {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ReportDetailActivity.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
            }
        }
    }

    /**
     * GeoLocate with the permission either granted or denied
     * @param granted true if granted
     */
    public void geolocateWithPermissions(boolean granted){
        if(granted) {
            try {
                Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (location != null) {
                    // get the users location and send it back
                    jsCallbackAfterPermissions.callback("{\"success\":true,\"lat\":\"" + location.getLatitude() + "\",\"lon\":\"" + location.getLongitude() + "\"}");
                } else {
                    jsCallbackAfterPermissions.callback("{\"success\":false,\"message\":\"DICE could not determine your location.  Ensure location services are enabled on your device.\"}");
                }
            }catch(SecurityException e){
                Log.e(TAG, "Failed to get user location after permissions were granted", e);
                jsCallbackAfterPermissions.callback("{\"success\":false,\"message\":\"DICE does not have location permissions to determine your location.\"}");
            }
        }else{
            jsCallbackAfterPermissions.callback("{\"success\":false,\"message\":\"DICE does not have location permissions to determine your location.\"}");
            // If the user has declared to no longer get asked about permissions
            if (!ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(mActivity, R.style.AppCompatAlertDialogStyle)
                        .setTitle(mActivity.getResources().getString(R.string.location_access_denied_title))
                        .setMessage(mActivity.getResources().getString(R.string.location_access_denied_message))
                        .setPositiveButton(R.string.settings, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", mActivity.getPackageName(), null));
                                mActivity.startActivity(intent);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        }
    }

    private void click(String data, WebViewJavascriptBridge.WVJBResponseCallback jsCallback){
        Log.i(TAG, "Bridge received request to query on a map click: " + data);
        if (jsCallback != null) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                double lat = jsonObject.getDouble("lat");
                double lon = jsonObject.getDouble("lng");
                double zoom = jsonObject.getDouble("zoom");
                JSONObject bounds = jsonObject.getJSONObject("bounds");

                if(bounds != null) {
                    LatLng location = new LatLng(lat, lon);

                    BoundingBox mapBounds = null;
                    JSONObject southWest = bounds.getJSONObject("_southWest");
                    JSONObject northEast = bounds.getJSONObject("_northEast");
                    if(southWest != null && northEast != null){
                        double minLon = southWest.getDouble("lng");
                        double maxLon = northEast.getDouble("lng");
                        double minLat = southWest.getDouble("lat");
                        double maxLat = northEast.getDouble("lat");
                        mapBounds = new BoundingBox(minLon, maxLon, minLat, maxLat);
                    }

                    if(mapBounds != null) {

                        // Include points by default
                        boolean includePoints =  jsonObject.optBoolean("points", true);

                        // Do not include geometries by default
                        boolean includeGeometries = jsonObject.optBoolean("geometries", true);

                        Map<String, Object> clickData = geoPackage.mapClickTableData(location, zoom, mapBounds, includePoints, includeGeometries);

                        if(clickData == null){
                            jsCallback.callback("{\"success\":true,\"message\":\"\"}");
                        }else{
                            try {
                                JSONObject jsonData = new JSONObject(clickData);
                                jsCallback.callback("{\"success\":true,\"message\":" + jsonData.toString() + "}");
                            }catch(Exception e2){
                                Log.e(JavaScriptAPI.class.getSimpleName(), "Failed to build JSON response", e2);
                                jsCallback.callback("{\"success\":false,\"message\":\"DICE failed to build JSON response. " + e2.getMessage() + "\"}");
                            }
                        }
                    }else{
                        jsCallback.callback("{\"success\":false,\"message\":\"Data bounds did not contain correct _southWest and _northWest values\"}");
                    }
                }else{
                    jsCallback.callback("{\"success\":false,\"message\":\"Data did not contain bounds value\"}");
                }

            }catch(JSONException e){
                Log.e(JavaScriptAPI.class.getSimpleName(), "Failed to parse JSON: " + data, e);
                jsCallback.callback("{\"success\":false,\"message\":\"DICE failed to parse JSON. " + e.getMessage() + "\"}");
            }
        }
    }

    public void removeFromWebView() {
        geoPackage.close();
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
