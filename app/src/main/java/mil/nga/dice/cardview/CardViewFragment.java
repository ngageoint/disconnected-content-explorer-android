package mil.nga.dice.cardview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mil.nga.dice.R;
import mil.nga.dice.report.ReportManager;


public class CardViewFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mAdapter = new CardAdapter(getActivity(), ReportManager.getInstance().getReports());
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity());
        final Handler uiThread = new Handler(Looper.getMainLooper());
        bm.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAdapter.notifyDataSetChanged();
            }
        }, new IntentFilter(ReportManager.INTENT_UPDATE_REPORT_LIST));
        bm.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (swipeRefresh == null) {
                    return;
                }
                uiThread.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefresh.setRefreshing(swipeRefresh.isRefreshing());
                    }
                }, 250); // pause for effect
                swipeRefresh.setRefreshing(false);
            }
        }, new IntentFilter(ReportManager.INTENT_END_REFRESH_REPORT_LIST));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_report_recycler, container, false);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.report_recycler);
        mRecyclerView.setHasFixedSize(true);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float widthDp = metrics.widthPixels / metrics.density;
        swipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.report_collection_swipe_refresh);
        swipeRefresh.setOnRefreshListener(this);

        // Smaller screens get a list of cards, larger screens get a grid of cards.
        if (widthDp < 700) {
            mLayoutManager = new LinearLayoutManager(getActivity());
        } else {
            int columns = 2;
            if (widthDp > 900)  {
                columns = 3;
            }
            mLayoutManager = new GridLayoutManager(getActivity(), columns);
        }
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        return v;
    }

    @Override
    public void onRefresh() {
        ReportManager.getInstance().refreshReports();
    }
}
