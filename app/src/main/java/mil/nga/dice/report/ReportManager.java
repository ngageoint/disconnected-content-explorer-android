package mil.nga.dice.report;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

// TODO: change to be a service?
public class ReportManager {

	public static final String INTENT_UPDATE_REPORT_LIST = ReportManager.class.getName() + ".UPDATE_REPORT_LIST";

	public static class Configuration {
		private boolean ready = false;
		private Configuration(Application host) {
			if (host == null) {
				throw new IllegalArgumentException("host application cannot be null");
			}
			instance.app = host;
			instance.handler = new Handler(host.getMainLooper());
		}
		private boolean isReady() {
			return ready;
		}
		public Configuration reportsDir(File x) {
			instance.reportsDir = x;
			return this;
		}
		public ReportManager finish() {
			ready = instance.reportsDir != null;
			return getInstance();
		}
	}

	public static synchronized Configuration initialize(Application host) {
		if (config != null) {
			throw new IllegalStateException("already initialized");
		}
		return config = new Configuration(host);
	}

	public static ReportManager getInstance() {
		if (config == null || !config.isReady()) {
			throw new IllegalStateException(instance.getClass().getName() + " has not been properly initialized");
		}
		return instance;
	}

	private static class BackgroundRunnable implements Runnable {
		private final Runnable target;
		BackgroundRunnable(Runnable target){
			this.target = target;
		}
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
			target.run();
		}
	}

	private static Configuration config;

	private static final int KEEP_ALIVE_TIME = 1;
	private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
	private static final int CORE_POOL_SIZE = 5;
	private static final int MAXIMUM_POOL_SIZE = 5;

	private static final ReportManager instance = new ReportManager();

	static final int LOAD_FAILED = -1;
	static final int LOAD_COMPLETE = 1;


	private final List<Report> reports = new ArrayList<>();
	private final List<Report> reportsView = Collections.unmodifiableList(reports);

	private Application app;
	private File reportsDir;
	private ThreadPoolExecutor reportProcessor;
	private Handler handler;

	private ReportManager() {
		reportProcessor = new ThreadPoolExecutor(
				CORE_POOL_SIZE,
				MAXIMUM_POOL_SIZE,
				KEEP_ALIVE_TIME,
				KEEP_ALIVE_TIME_UNIT,
				new LinkedBlockingQueue<Runnable>());
		final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();
		reportProcessor.setThreadFactory(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return defaultThreadFactory.newThread(new BackgroundRunnable(r));
			}
		});
	}


	/**
	 * Handle sending messages based on the state of a report task.
	 * @param report
	 * @param state
	 */
	public void handleState(Report report, int state) {
		switch (state) {
		case LOAD_COMPLETE:
			report.setEnabled(true);
			break;
		case LOAD_FAILED:
			report.setEnabled(false);
			report.setDescription("Problem loading report");
			break;
		}
		LocalBroadcastManager.getInstance(app).sendBroadcast(new Intent(INTENT_UPDATE_REPORT_LIST));
	}

	/**
	 * Return a live, read-only list of the processed reports.
	 * @return
	 */
	public List<Report> getReports() {				
		return reportsView;
	}
	
	public void processReports(File... reportFiles) {
		if (reportFiles == null) {
			throw new IllegalArgumentException("report file is null");
		}
		for (File reportFile : reportFiles) {
			String fileName = reportFile.getName();
			String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
			String simpleName = fileName.substring(0, fileName.lastIndexOf("."));

			Report report = new Report();
			report.setSourceFile(reportFile);
			report.setTitle(simpleName);
			report.setDescription("Processing report ...");
			report.setEnabled(false);

			if (extension.equals("zip")) {
				File unzipDir = new File(reportsDir, simpleName);
				report.setPath(unzipDir);
				addReport(report);
				// TODO: need to refactor this to use the threadpool properly
				startUnzip(report);
			}
			else if (extension.equals("pdf")) {
			 	// TODO: need to look into more PDF options on android
				report.setEnabled(true);
				report.setDescription("");
				report.setPath(new File(reportsDir.getPath(), report.getFileName()));
				// TODO: copy pdf to new location
				addReport(report);
			}
			else if (extension.equalsIgnoreCase("docx")) {
				// TODO: word files
			}
			else if (extension.equalsIgnoreCase("pptx")) {
				// TODO: powerpoint files
			}
			else if (extension.equalsIgnoreCase("xlsx")) {
				// TODO: excel files
			}
		}
	}

	private void startUnzip(Report report) {
		reportProcessor.execute(new ReportUnzipRunnable(report));
	}
	
	public Report getReportWithID(String id) {
		for (Report r : reports) {
			if (r.getId() != null && r.getId().equals(id)) {
				return r;
			}
		}
		return null;
	}

	private class AddReportOnUIThread implements Runnable {
		private final Report report;
		private AddReportOnUIThread(Report report) {
			this.report = report;
		}
		@Override
		public void run() {
			reports.add(report);
			LocalBroadcastManager.getInstance(app).sendBroadcastSync(new Intent(INTENT_UPDATE_REPORT_LIST));
		}
	}

	private void addReport(Report report) {
		handler.post(new AddReportOnUIThread(report));
	}
	
	public File getReportsDir() {
		return reportsDir;
	}

}
