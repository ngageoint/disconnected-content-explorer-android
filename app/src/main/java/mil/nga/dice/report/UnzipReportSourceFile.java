package mil.nga.dice.report;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extract report content from its Zip {@link mil.nga.dice.report.Report#getSourceFile() source file}.  Also update the report properties if the Zip
 * contains a metadata.json file.  Progress notifications go to the given {@link mil.nga.dice.report.ReportImportCallbacks callbacks} object
 * on the main thread using the {@link android.os.AsyncTask} API.
 */
public class UnzipReportSourceFile extends AsyncTask<Void, Integer, Void> {

	private static final String TAG = UnzipReportSourceFile.class.getSimpleName();

	private static final int BUFFER_SIZE = 1 << 16;

	private final Report report;
    private final File destDir;

    private Context context;
    private ReportImportCallbacks callbacks;
    private long entryBytesRead = 0;
    private long totalEntryBytes = -1;
    private int percentComplete = 0;
    private Exception error = null;
	
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
		}
		catch (Exception e) {
            error = e;
			Log.e(TAG, "error unzipping report file: " + report.getSourceFile(), e);
		}

        return null;
	}

    @Override
    protected void onProgressUpdate(Integer... values) {
        callbacks.importProgressPercentage(report, values[0]);
    }

    @Override
    protected void onPostExecute(Void nothing) {
        if (error == null) {
            callbacks.importComplete(report);
        }
        else {
            callbacks.importError(report);
        }
        context = null;
        callbacks = null;
    }

    private void unzip() throws IOException {
		byte[] entryBuffer = new byte[BUFFER_SIZE];
		ZipInputStream zipIn = new ZipInputStream(
                new CompressedByteReportingInputStream(
                        context.getContentResolver().openInputStream(report.getSourceFile())));

		try {
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				File entryFile = new File(destDir, entry.getName());
				if (entry.isDirectory()) {
					entryFile.mkdir();
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
            int roundedPercent = (int) Math.round((double) entryBytesRead / totalEntryBytes * 100);
            if (roundedPercent > percentComplete) {
                percentComplete = roundedPercent;
                publishProgress(percentComplete);
            }
		}
		entryOut.close();
	}

    private class CompressedByteReportingInputStream extends FilterInputStream {

        /**
         * Constructs a new {@code FilterInputStream} with the specified input
         * stream as source.
         * <p/>
         * <p><strong>Warning:</strong> passing a null source creates an invalid
         * {@code FilterInputStream}, that fails on every method that is not
         * overridden. Subclasses should check for null in their constructors.
         *
         * @param zipFileStream the input stream to filter reads on.
         */
        private CompressedByteReportingInputStream(InputStream zipFileStream) {
            super(zipFileStream);
        }

        @Override
        public int read() throws IOException {
            entryBytesRead++;
            return super.read();
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int readCount = super.read(buffer, byteOffset, byteCount);
            entryBytesRead += readCount;
            return readCount;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int readCount = super.read(buffer);
            entryBytesRead += readCount;
            return readCount;
        }

        @Override
        public long skip(long byteCount) throws IOException {
            long skipCount = super.skip(byteCount);
            entryBytesRead += skipCount;
            return skipCount;
        }
    }
}
