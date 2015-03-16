package mil.nga.dice.cardview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mil.nga.dice.R;
import mil.nga.dice.report.ReportManager;


public class CardViewFragment extends android.support.v4.app.Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        mAdapter = new CardAdapter(getActivity(), ReportManager.getInstance().getReports());
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity());
        bm.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAdapter.notifyDataSetChanged();
            }
        }, new IntentFilter(ReportManager.INTENT_UPDATE_REPORT_LIST));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_report_recycler, container, false);
        mRecyclerView = (RecyclerView) v.findViewById(R.id.report_recycler);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        return v;
    }
}
