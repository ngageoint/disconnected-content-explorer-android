package mil.nga.dice.report;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extract report content from its Zip {@link mil.nga.dice.report.Report#getSourceFile() source file}.  Also update the report properties if the Zip
 * contains a metadata.json file.  Progress notifications go to the given {@link mil.nga.dice.report.UnzipReportSourceFile.Callbacks callbacks} object
 * on the main thread using the {@link android.os.AsyncTask} API.
 */
public class UnzipReportSourceFile extends AsyncTask<Void, Integer, Void> {

	private static final String TAG = UnzipReportSourceFile.class.getSimpleName();

	private static final int BUFFER_SIZE = 1 << 16;

    public static interface Callbacks {
        void unzipPercentageComplete(Report report, int percent);
        void unzipComplete(Report report);
    }

	private final Report report;
    private final File destDir;

    private Context context;
    private ReportImportCallbacks callbacks;
    private long entryBytesRead = 0;
    double totalEntryBytes = -1.0;
	
	public UnzipReportSourceFile(Report report, File destDir, Context context, ReportImportCallbacks callbacks) {
		this.report = report;
        this.destDir = destDir;
        this.context = context;
        this.callbacks = callbacks;

        if (report.getSourceFileSize() > 0) {
            totalEntryBytes = report.getSourceFileSize();
        }
	}
	
	/**
	 * The bits that will be run on a thread
	 */
	@Override
	public Void doInBackground(Void... params) {
		try {
			Log.i(TAG, "unzipping report package " + report.getSourceFile());
            unzip();
            ReportDescriptorUtil.readDescriptorAndUpdateReport(report);
		}
		catch (Exception e) {
			Log.e(TAG, "error unzipping report file: " + report.getSourceFile(), e);
			report.setDescription("Error unzipping report");
			report.setEnabled(false);
		}

        return null;
	}

    @Override
    protected void onProgressUpdate(Integer... values) {
        callbacks.percentageComplete(report, values[0]);
    }

    @Override
    protected void onPostExecute(Void nothing) {
        callbacks.importComplete(report);

        context = null;
        callbacks = null;
    }

    private void unzip() throws IOException {
		byte[] entryBuffer = new byte[BUFFER_SIZE];
		ZipInputStream zipIn = new ZipInputStream(context.getContentResolver().openInputStream(report.getSourceFile()));
		try {
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				File entryFile = new File(destDir, entry.getName());
				if (entry.isDirectory()) {
					entryFile.mkdir();
                    entryBytesRead += entry.getCompressedSize();
				}
				else {
					extractEntryFile(zipIn, entryFile, entryBuffer);
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
		}
		finally {
			zipIn.close();
		}
	}

	private void extractEntryFile(ZipInputStream zipIn, File toFile, byte[] entryBuffer) throws IOException {
		OutputStream entryOut = new FileOutputStream(toFile);
		int read;
		while ((read = zipIn.read(entryBuffer)) != -1) {
			entryOut.write(entryBuffer, 0, read);
            entryBytesRead += read;
            publishProgress((int) Math.round((double) entryBytesRead / totalEntryBytes));

		}
		entryOut.close();
	}


}
