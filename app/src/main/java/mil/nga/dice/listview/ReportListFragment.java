package mil.nga.dice.listview;

import android.app.Activity;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ListView;

import mil.nga.dice.ReportCollectionCallbacks;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;

/**
 * Activities containing this fragment MUST implement the {@link mil.nga.dice.ReportCollectionCallbacks}
 * interface.
 */
public class ReportListFragment extends ListFragment implements ReportManager.ReportManagerClient {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";


    private CustomList mReportsAdapter;
	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;
    private ReportCollectionCallbacks mCallbacks;
    private ReportManager reportManager;


	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ReportListFragment() {
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState
					.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof ReportCollectionCallbacks)) {
			throw new IllegalStateException(
					"parent activity must implement " + ReportCollectionCallbacks.class.getName());
		}

		mCallbacks = (ReportCollectionCallbacks) activity;
	}


	@Override
	public void onDetach() {
		super.onDetach();

		mCallbacks = null;
	}

	
	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		mCallbacks.reportSelectedToView((Report) mReportsAdapter.getItem(position));
	}

	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}


    @Override
    public void reportManagerConnected(ReportManager.Connection x) {
        reportManager = x.getReportManager();

        mReportsAdapter = new CustomList(getActivity(), reportManager.getReports());
        setListAdapter(mReportsAdapter);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
        bm.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mReportsAdapter.notifyDataSetChanged();
            }
        }, new IntentFilter(ReportManager.INTENT_UPDATE_REPORT_LIST));
    }


    @Override
    public void reportManagerDisconnected() {
        reportManager = null;
    }


	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		}
		else {
			getListView().setItemChecked(position, true);
		}
		mActivatedPosition = position;
	}
}
