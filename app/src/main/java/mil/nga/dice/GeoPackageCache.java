package mil.nga.dice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.InputStream;

import mil.nga.dice.io.DICEFileUtils;
import mil.nga.dice.map.geopackage.GeoPackageSelected;
import mil.nga.dice.report.ReportManager;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.io.GeoPackageIOUtils;
import mil.nga.geopackage.io.GeoPackageProgress;
import mil.nga.geopackage.validate.GeoPackageValidate;

/**
 * GeoPackage cache for importing GeoPackage files
 */
public class GeoPackageCache {

    /**
     * Creating activity
     */
    private final Activity activity;

    /**
     * GeoPackage manager
     */
    private final GeoPackageManager manager;

    /**
     * Progress dialog for network operations
     */
    private ProgressDialog progressDialog;

    /**
     * Import external GeoPackage name holder when asking for external write permission
     */
    private String importExternalName;

    /**
     * Import external GeoPackage URI holder when asking for external write permission
     */
    private Uri importExternalUri;

    /**
     * Import external GeoPackage path holder when asking for external write permission
     */
    private String importExternalPath;

    /**
     * Selected GeoPackages
     */
    private GeoPackageSelected selected;

    /**
     * Constructor
     *
     * @param activity
     */
    public GeoPackageCache(Activity activity) {
        this.activity = activity;
        this.manager = GeoPackageFactory.getManager(activity);
        this.selected = new GeoPackageSelected(activity);
    }

    /**
     * Check if the name contains a GeoPackage extension
     *
     * @param name file name
     * @return true if a GeoPackage name
     */
    public boolean hasGeoPackageExtension(String name) {
        return GeoPackageValidate.hasGeoPackageExtension(new File(name));
    }

    /**
     * Import the GeoPackage file, by copying a remote file or linking a local file
     *
     * @param name database name
     * @param uri file uri
     * @param path file path
     */
    public void importFile(String name, Uri uri, String path) {
        name = DICEFileUtils.removeExtension(name);
        if (path == null || DICEFileUtils.isTemporaryPath(path)) {
            importGeoPackage(name, uri, path);
        } else {
            importGeoPackageExternalLinkWithPermissions(name, uri, path);
        }
    }

    /**
     * Import a GeoPackage as an externally linked file and handling permission requests
     *
     * @param name database name
     * @param uri file uri
     * @param path file path
     */
    private void importGeoPackageExternalLinkWithPermissions(final String name, final Uri uri, String path) {

        // Check for permission
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            importGeoPackageExternalLink(name, uri, path);
        } else {

            // Save off the values and ask for permission
            importExternalName = name;
            importExternalUri = uri;
            importExternalPath = path;

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
                        .setTitle(R.string.storage_access_rational_title)
                        .setMessage(R.string.storage_access_rational_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, ReportCollectionActivity.PERMISSIONS_REQUEST_IMPORT_GEOPACKAGE);
                            }
                        })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, ReportCollectionActivity.PERMISSIONS_REQUEST_IMPORT_GEOPACKAGE);
            }
        }

    }

    /**
     * Import the GeoPackage by linking to the file after write external storage permission was granted
     *
     * @param granted true if permission was granted
     */
    public void importGeoPackageExternalLinkAfterPermissionGranted(boolean granted) {
        if (granted) {
            importGeoPackageExternalLink(importExternalName, importExternalUri, importExternalPath);
        } else {
            showDisabledExternalImportPermissionsDialog();
        }
    }

    /**
     * Import the GeoPackage by linking to the file
     *
     * @param name database name
     * @param uri file uri
     * @param path file path
     */
    private void importGeoPackageExternalLink(final String name, final Uri uri, String path) {

        if(manager.importGeoPackageAsExternalLink(path, name, true)){
            selected.addSelected(name);
        }

    }

    /**
     * Run the import task to import a GeoPackage by copying it
     *
     * @param name database name
     * @param uri file uri
     * @param path file path
     */
    private void importGeoPackage(final String name, Uri uri, String path) {

        if(manager.exists(name)){
            manager.delete(name);
            selected.removeSelected(name);
        }

        ImportTask importTask = new ImportTask(name, path, uri);
        progressDialog = createImportProgressDialog(name,
                importTask, path, uri, null);
        progressDialog.setIndeterminate(true);
        importTask.execute();

    }


    /**
     * Import a GeoPackage from a stream in the background
     */
    private class ImportTask extends AsyncTask<String, Integer, String>
            implements GeoPackageProgress {

        private Integer max = null;
        private int progress = 0;
        private final String database;
        private final String path;
        private final Uri uri;
        private PowerManager.WakeLock wakeLock;

        /**
         * Constructor
         *
         * @param database
         * @param path
         * @param uri
         */
        public ImportTask(String database, String path, Uri uri) {
            this.database = database;
            this.path = path;
            this.uri = uri;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setMax(int max) {
            this.max = max;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addProgress(int progress) {
            this.progress += progress;
            if (max != null) {
                int total = (int) (this.progress / ((double) max) * 100);
                publishProgress(total);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isActive() {
            return !isCancelled();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean cleanupOnCancel() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
            PowerManager pm = (PowerManager) activity.getSystemService(
                    Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            wakeLock.acquire();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);

            // If the indeterminate progress dialog is still showing, swap to a
            // determinate horizontal bar
            if (progressDialog.isIndeterminate()) {

                String messageSuffix = "\n\n"
                        + GeoPackageIOUtils.formatBytes(max);

                ProgressDialog newProgressDialog = createImportProgressDialog(
                        database, this, path, uri, messageSuffix);
                newProgressDialog
                        .setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                newProgressDialog.setIndeterminate(false);
                newProgressDialog.setMax(100);

                newProgressDialog.show();
                progressDialog.dismiss();
                progressDialog = newProgressDialog;
            }

            // Set the progress
            progressDialog.setProgress(progress[0]);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onCancelled(String result) {
            wakeLock.release();
            progressDialog.dismiss();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void onPostExecute(String result) {
            wakeLock.release();
            progressDialog.dismiss();
            if (result != null) {
                showMessage("Import",
                        "Failed to import: "
                                + (path != null ? path : uri.getPath()));
            }else{
                selected.addSelected(database);
                ReportManager.getInstance().refreshReports(activity);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String doInBackground(String... params) {
            try {
                final ContentResolver resolver = activity.getContentResolver();
                InputStream stream = resolver.openInputStream(uri);
                if (!manager.importGeoPackage(database, stream, true, this)) {
                    String message = "Failed to import GeoPackage '" + database + "'";
                    Log.e(GeoPackageCache.class.getSimpleName(), message);
                    return message;
                }
            } catch (final Exception e) {
                Log.e(GeoPackageCache.class.getSimpleName(), "Failed to import GeoPackage. URI: " + uri.getPath(), e);
                return e.toString();
            }
            return null;
        }

    }

    /**
     * Create a import progress dialog
     *
     * @param database
     * @param importTask
     * @param path
     * @param uri
     * @param suffix
     * @return
     */
    private ProgressDialog createImportProgressDialog(String database, final ImportTask importTask,
                                                      String path, Uri uri,
                                                      String suffix) {
        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setMessage(activity.getString(R.string.import_label) + " "
                + database + "\n\n" + (path != null ? path : uri.getPath()) + (suffix != null ? suffix : ""));
        dialog.setCancelable(false);
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE,
                activity.getString(R.string.button_cancel_label),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        importTask.cancel(true);
                    }
                });
        return dialog;
    }

    /**
     * Show a message with an OK button
     *
     * @param title
     * @param message
     */
    private void showMessage(String title,
                             String message) {
        if (title != null || message != null) {
            new AlertDialog.Builder(activity)
                    .setTitle(title != null ? title : "")
                    .setMessage(message != null ? message : "")
                    .setNeutralButton(
                            activity.getString(R.string.button_ok_label),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    /**
     * Show a disabled read external storage permissions dialog when external files can not be read
     */
    private void showDisabledExternalImportPermissionsDialog() {
        // If the user has declared to no longer get asked about permissions
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showDisabledPermissionsDialog(
                    activity.getResources().getString(R.string.read_external_access_denied_title),
                    activity.getResources().getString(R.string.read_external_access_denied_message));
        }
    }

    /**
     * Show a disabled permissions dialog
     *
     * @param title
     * @param message
     */
    private void showDisabledPermissionsDialog(String title, String message) {
        new android.support.v7.app.AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.settings, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
                        activity.startActivityForResult(intent, ReportCollectionActivity.ACTIVITY_APP_SETTINGS);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}
