package mil.nga.dice.cardview;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionCallbacks;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;


public class CardViewFragment extends android.support.v4.app.Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout swipeRefresh;
    private String mClipText = "";
    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        mAdapter = new CardAdapter(getActivity(), ReportManager.getInstance().getReports(), (ReportCollectionCallbacks) getActivity());

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

        mPreferences = getActivity().getSharedPreferences("mil.nga.dice", Context.MODE_PRIVATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkClipboard();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
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
        initItemTouchHelper();

        ReportManager.getInstance().refreshReports(getActivity());
        return v;
    }

    @Override
    public void onRefresh() {
        ReportManager.getInstance().refreshReports(getActivity());
    }


    private void checkClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboardManager.getPrimaryClip();

        if (clip != null) {
            try {
                mClipText = (String) clip.getItemAt(0).getText();

                String previousURL = mPreferences.getString(getString(R.string.previous_url_key), getString(R.string.default_clipboard_value));
                if (!previousURL.equals(mClipText)) {
                    ClipboardURLCheckTask urlCheckTask = new ClipboardURLCheckTask();
                    urlCheckTask.execute(mClipText);
                }
            } catch (Exception e) {
                Log.e("CardViewFragment", e.getLocalizedMessage());
            }
        }
    }


    private void initItemTouchHelper() {
        Log.d("CardViewFragment", "Initializeing touch helper.");

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            Drawable background;
            Drawable deleteIcon;
            int deleteIconMargin;
            boolean isSetup = false;

            private void init() {
                background = new ColorDrawable(Color.RED);
                deleteIcon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete_forever_black_24dp);
                deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                deleteIconMargin = (int) getActivity().getResources().getDimension(R.dimen.x_icon_margin);
                isSetup = true;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }


            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                CardAdapter adapter = (CardAdapter)recyclerView.getAdapter();

                // disable the swipe action for the default placeholder report.
                Report report = ReportManager.getInstance().getReports().get(position);
                if( !report.isEnabled() || (report.getId() != null && report.getId().equals(ReportManager.USER_GUIDE_REPORT_ID))) {
                    return 0;
                }

                int swipeFlags = ItemTouchHelper.LEFT;

                return swipeFlags;
            }


            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                Log.d("CardViewFragment","Swipe detected");
                int swipedPosition = viewHolder.getAdapterPosition();
                CardAdapter adapter = (CardAdapter)mRecyclerView.getAdapter();

                adapter.swiped(swipedPosition, getActivity());
            }


            @Override
            public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                if (viewHolder.getAdapterPosition() == -1) {
                    return;
                }

                if (!isSetup) {
                    init();
                }

                background.setBounds(itemView.getRight() + (int)dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(canvas);

                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = deleteIcon.getIntrinsicWidth();
                int intrinsicHeight = deleteIcon.getIntrinsicHeight();

                int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
                int deleteIconRight = itemView.getRight() - deleteIconMargin;
                int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
                int deleteIconBottom = deleteIconTop + intrinsicHeight;

                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                deleteIcon.draw(canvas);
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }


    /**
     * Check and see if the user has a URL in their clipboard. If they do, is it
     * something that DICE can consume? If so, lets offer to download it for them.
     */
    private class ClipboardURLCheckTask extends AsyncTask<String, String, String> {

        URL mUrl;
        boolean mIsReport = false;

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected String doInBackground(String ...uri) {
            String responseString = null;

            try{
                mUrl = new URL(uri[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) mUrl.openConnection();
                urlConnection.setRequestMethod("HEAD");
                urlConnection.connect();
                responseString = urlConnection.getResponseMessage();

                for (Map.Entry<String, List<String>> k : urlConnection.getHeaderFields().entrySet()) {
                    Log.d("ClipboardURLCheckTask", k.toString());
                }

                String contentType = urlConnection.getHeaderField("Content-Type");
                String mimeType = urlConnection.getHeaderField("MIMEType");

                if (mimeType != null && mimeType.equals("application/zip") || contentType != null && (contentType.equals("application/zip") || contentType.equals("application/octet-stream"))) {
                    Log.d("ClipboardURLCheckTask", "URL Looks good");
                    mIsReport = true;
                }

            } catch (Exception e) {
                Log.e("ClipboardURLCheckTask", e.getLocalizedMessage());
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            if (mIsReport) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.downaload_dialog_title);
                builder.setMessage(mUrl.toString());

                builder.setPositiveButton(R.string.download_dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ReportManager.getInstance().downloadReport(mUrl, getActivity());
                    }
                });

                builder.setNegativeButton(R.string.download_dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: close the dialog
                    }
                });

                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putString(getString(R.string.previous_url_key), mUrl.toString());
                editor.commit();

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }
}
