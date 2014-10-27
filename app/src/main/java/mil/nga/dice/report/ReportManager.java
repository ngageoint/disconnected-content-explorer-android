package mil.nga.dice.report;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import mil.nga.dice.listview.ReportListFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReportManager {
	// Time units, and thread count for threading
	private static final int KEEP_ALIVE_TIME = 1;
	private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
	private static final int CORE_POOL_SIZE = 8;
	private static final int MAXIMUM_POOL_SIZE = 8;

	private static final ReportManager sInstance = new ReportManager();

	// status indicators
	static final int LOAD_FAILED = -1;
	static final int LOAD_COMPLETE = 1;

	// A work queue, a pool to work from, and a way back to the UI thread
	private final ThreadPoolExecutor mReportLoadingPool;
	// The list of reports
	private final List <Report> mReports = new ArrayList<>();
	private final Handler mHandler;

	// TODO: referencing a ui class here smells
	private ReportListFragment mReportListFragment;
	private File root = Environment.getExternalStorageDirectory();
	private File diceRoot = new File(root.getPath(), "DICE");

	private ReportManager () {
		super();

		mReportLoadingPool = new ThreadPoolExecutor(
				CORE_POOL_SIZE,
				MAXIMUM_POOL_SIZE,
				KEEP_ALIVE_TIME,
				KEEP_ALIVE_TIME_UNIT,
				new LinkedBlockingQueue<Runnable>());
		
		/*
		 * Create a new handler so we can talk back to the UI thread
		 */
		mHandler = new Handler(Looper.getMainLooper()) {
			public void handleMessage(Message inputMessage) {
				Report report = (Report) inputMessage.obj;
				ArrayList<Report> tmpReports = new ArrayList<>(mReportListFragment.getReports());
						
				int listSize = tmpReports.size();
				for (int i = 0; i < listSize; i++) {
					if(tmpReports.get(i).getFilename().equals(report.getFilename())) {
						tmpReports.set(i, report);
					}
				}
				
				mReportListFragment.setReportList(tmpReports);
				mReportListFragment.refreshListAdapter();
			}
		};
	}

	/**
	 * Returns the ReportManager object
	 * @return The global ReportManager object
	 */
	public static ReportManager getInstance() {
		return sInstance;
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
				Message completeMessage = mHandler.obtainMessage(state, report);
				completeMessage.sendToTarget();
				break;
			case LOAD_FAILED:
				report.setEnabled(false);
				report.setDescription("Problem loading report ...");
			default:
				mHandler.obtainMessage(state, report).sendToTarget();
				break;
		}
	}

	public List<Report> getReports() {				
		return mReports;
	}
	
	public void loadReports() {
		mReports.clear();

		if (!diceRoot.exists()) {
			return;
		}

		/*
		 * TODO: use File objects for report files and retain the full path on the Report object and
		 * eliminate the need for client classes to call getDiceRoot()
		 */
		String listOfFiles[] = diceRoot.list();

		for (String filename : listOfFiles) {
			String extension = filename.substring(filename.lastIndexOf(".") + 1);
			Report report = new Report();
			report.setFilename(filename);
			report.setDescription("Unzipping report...");
			report.setFileExtension(extension);
			report.setEnabled(false);

			if (extension.equals("zip")) {
				String unzipDirName = filename.substring(0, filename.lastIndexOf("."));
				report.setTitle(unzipDirName);
				File unzipDir = new File(diceRoot, unzipDirName);
				report.setPath(unzipDir.getAbsolutePath());
				this.addReport(report);
				// TODO: need to refactor this to use the threadpool properly
				startUnzip(report);
			}
			else if (extension.equals("pdf")) {
			 	// TODO: need to look into more PDF options on android
				report.setTitle(filename);
				report.setEnabled(true);
				report.setDescription("");
				report.setPath(diceRoot.getPath() + File.separator + filename);
				this.addReport(report);
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
		mReportLoadingPool.execute(new ReportUnzipRunnable(report));
	}
	
	public Report getReportWithID(String id) {
		for (Report r : mReports) {
			if (r.getId() != null && r.getId().equals(id)) {
				return r;
			}
		}
		return null;
	}
	
	private void addReport(Report report) {
		mReports.add(report);
	}
	
	public List<Report> getReportList() {
		return mReports;
	}

	public File getDiceRoot() {
		return diceRoot;
	}

	public void setReportListFragment(ReportListFragment reportListFragment) {
		mReportListFragment = reportListFragment;
	}
}
