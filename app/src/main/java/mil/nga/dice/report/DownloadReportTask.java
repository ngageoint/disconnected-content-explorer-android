package mil.nga.dice.report;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 */
public class DownloadReportTask extends AsyncTask<String, Integer, Void> {

    private Report mReport;
    private Activity mActivity;
    private ReportImportCallbacks mCallbacks;
    private Uri mReportZipPath;


    public DownloadReportTask(Report report, Activity activity, ReportImportCallbacks callbacks) {
        mReport = report;
        mActivity = activity;
        mCallbacks = callbacks;
    }

    @Override
    protected Void doInBackground(String ...params) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        HttpURLConnection connection = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection)url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                mCallbacks.downloadError(mReport, "Unexpected response from server: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }

            // this can be -1 if the server doesn't report content length
            int fileLength = connection.getContentLength();
            inputStream = connection.getInputStream();

            String filename = URLUtil.guessFileName(params[0], connection.getHeaderField("Content-Disposition"), connection.getHeaderField("MIMEType"));
            mReportZipPath = Uri.fromFile(new File(ReportUtils.getReportDirectory() + "/" + filename));

            outputStream = new FileOutputStream(ReportUtils.getReportDirectory() + "/" + filename);

            byte data[] = new byte[4096];
            long total = 0;
            int count;

            while ((count = inputStream.read(data)) != -1) {
                total += count;

                if (fileLength > 0 && total % 100 == 0) {
                    double percent =  (((double)total / (double)fileLength) * 100);
                    Integer percentComplete = (int)percent;
                    publishProgress(percentComplete);
                }

                outputStream.write(data, 0, count);
            }

        } catch (Exception e) {
            Log.e("DownloadReportTask", e.getLocalizedMessage());
            mCallbacks.downloadError(mReport, e.getLocalizedMessage());
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mCallbacks.downloadProgressPercentage(mReport, values[0]);
    }

    protected void onPostExecute(Void nothing) {
        mCallbacks.downloadComplete(mReport, mActivity, mReportZipPath);
    }
}
