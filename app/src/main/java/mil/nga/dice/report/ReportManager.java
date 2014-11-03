package mil.nga.dice.report;

import android.app.Application;
import android.os.*;
import android.os.Process;
import mil.nga.dice.listview.ReportListFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

// TODO: change to be a service?
public class ReportManager {

	public static class Configuration {
		private final Application host;
		private boolean ready = false;
		private Configuration(Application x) {
			host = x;
			if (host == null) {
				throw new IllegalArgumentException("host application cannot be null");
			}
		}
		private boolean isReady() {
			return ready;
		}
		public Configuration reportsDir(File x) {
			instance.reportsDir = x;
			return this;
		}
		public ReportManager finish() {
			ready = true;
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

	// Time units, and thread count for threading
	private static final int KEEP_ALIVE_TIME = 1;
	private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
	private static final int CORE_POOL_SIZE = 5;
	private static final int MAXIMUM_POOL_SIZE = 5;

	private static final ReportManager instance = new ReportManager();

	// status indicators
	static final int LOAD_FAILED = -1;
	static final int LOAD_COMPLETE = 1;


	private File reportsDir = new File(Environment.getExternalStorageDirectory(), "DICE");
	private final List <Report> reports = new ArrayList<>();

	// A work queue, a pool to work from, and a way back to the UI thread
	private ThreadPoolExecutor reportProcessor;
	private Handler handler;
	// TODO: referencing a ui class here smells
	private ReportListFragment reportListFragment;

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

		handler = new Handler(Looper.getMainLooper()) {
			public void handleMessage(Message inputMessage) {
				if (reportListFragment == null) {
					return;
				}

				Report report = (Report) inputMessage.obj;
				inputMessage.replyTo = null;
				ArrayList<Report> tmpReports = new ArrayList<>(reportListFragment.getReports());

				int listSize = tmpReports.size();
				for (int i = 0; i < listSize; i++) {
					if (tmpReports.get(i).getFileName().equals(report.getFileName())) {
						tmpReports.set(i, report);
					}
				}

				reportListFragment.setReportList(tmpReports);
				reportListFragment.refreshListAdapter();
			}
		};
	}



	/**
	 * Handle sending messages based on the state of a report task.
	 * @param report
	 * @param state
	 */
	public void handleState(Report report, int state) {
		Message completeMessage = handler.obtainMessage(state, report);
		switch (state) {
		case LOAD_COMPLETE:
			report.setEnabled(true);
			break;
		case LOAD_FAILED:
			report.setEnabled(false);
			report.setDescription("Problem loading report");
			break;
		}
		completeMessage.sendToTarget();
	}

	/**
	 * Return a read-only list of the processed reports.
	 * @return
	 */
	public List<Report> getReports() {				
		return Collections.unmodifiableList(reports);
	}
	
	public void processReports(File... reportFiles) {
		if (!reportsDir.isDirectory() && !reportsDir.mkdirs()) {
			throw new IllegalStateException(
					"report directory path is not a directory and could not be created: " +
							reportsDir.getAbsolutePath());
		}
		if (reportFiles == null) {
			throw new IllegalArgumentException("report path is null");
		}
		/*
		 * TODO: use File objects for report files and retain the full path on the Report object and
		 * eliminate the need for client classes to call getReportsDir()
		 */
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

	private void addReport(Report report) {
		reports.add(report);
		// TODO: send broadcast message to ui components
	}
	
	public File getReportsDir() {
		return reportsDir;
	}

	public void setReportListFragment(ReportListFragment reportListFragment) {
		this.reportListFragment = reportListFragment;
	}
}
