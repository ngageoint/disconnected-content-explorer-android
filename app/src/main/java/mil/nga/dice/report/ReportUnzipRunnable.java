package mil.nga.dice.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

import android.util.Log;

/**
 * This runnable unzips a DICE report.
 */
public class ReportUnzipRunnable implements Runnable {

	private static final String tag = ReportUnzipRunnable.class.getSimpleName();

	private static final int BUFFER_SIZE = 1 << 16;

	final ReportManager reportManager = ReportManager.getInstance();
	final Report report;

	
	ReportUnzipRunnable(Report report) {
		this.report = report;
	}
	
	/**
	 * The bits that will be run on a thread
	 */
	@Override
	public void run() {
		String unzipDirName = report.getFileName().substring(0, report.getFileName().lastIndexOf("."));
		File reportContentRoot = new File(ReportManager.getInstance().getReportsDir(), unzipDirName);

		try {
			Log.i(tag, "unzipping report package " + report.getSourceFile());
			if (!reportContentRoot.exists()) {
				unzip(report.getSourceFile(), ReportManager.getInstance().getReportsDir());
			}

			// handle the metadata.json file to fancy up the report object for the list/grid/map views
			File metadataFile = new File(reportContentRoot, "metadata.json");

			if (metadataFile.exists()) {
				String jsonString = null;
				FileInputStream stream = new FileInputStream(metadataFile);

				try {
					FileChannel fc = stream.getChannel();
					MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
					jsonString = Charset.defaultCharset().decode(byteBuffer).toString();
				}
				finally {
					stream.close();
				}

				JSONObject jsonObject = new JSONObject(jsonString);
				if (jsonObject.has("title")) {
					report.setTitle(jsonObject.getString("title"));
				}
				if (jsonObject.has("description")) {
					report.setDescription(jsonObject.getString("description"));
				}
				else {
					report.setDescription(null);
				}
				if (jsonObject.has("reportID")) {
					report.setId(jsonObject.getString("reportID"));
				}
				if (jsonObject.has("lat")) {
					report.setLat(jsonObject.getDouble("lat"));
				}
				if (jsonObject.has("lon")) {
					report.setLon(jsonObject.getDouble("lon"));
				}
				if (jsonObject.has("thumbnail")) {
					report.setThumbnail(jsonObject.getString("thumbnail"));
				}
			}
		}
		catch (Exception e) {
			Log.i("ReportUnzipRunnable", "error unzipping report file " +
					report.getSourceFile() + ": " + e.getLocalizedMessage());
			report.setDescription("Error unzipping report");
			report.setEnabled(false);
		}

		reportManager.handleState(report, ReportManager.LOAD_COMPLETE);
	}
	
	
	private void unzip(File zipFile, File toDir) throws IOException {
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile));
		try {
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				File entryFile = new File(toDir, entry.getName());
				if (entry.isDirectory()) {
					entryFile.mkdir();
				}
				else {
					extractEntryFile(zipIn, entryFile);
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
		}
		finally {
			zipIn.close();
		}
	}
	
	
	private void extractEntryFile(ZipInputStream zipIn, File toFile) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(toFile));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
}
