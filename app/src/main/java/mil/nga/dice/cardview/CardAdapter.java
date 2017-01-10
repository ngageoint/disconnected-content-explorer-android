package mil.nga.dice.cardview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionCallbacks;
import mil.nga.dice.report.NoteActivity;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private final Context context;
    private final List<Report> reports;
    private List<Report> swipedReports;
    private final ReportCollectionCallbacks callbacks;
    private final LayoutInflater inflater;
    private Activity activityToUpdate;

    public CardAdapter(Context context, List<Report> reports, ReportCollectionCallbacks callbacks) {
        this.reports = reports;
        this.context = context;
        this.callbacks = callbacks;
        this.swipedReports = new ArrayList<>();
        inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getItemCount() {
        return reports.size();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.report_card, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Report report = reports.get(position);

        if (this.swipedReports.contains(report)) {
            holder.mDeleteButton.setVisibility(View.VISIBLE);
            holder.mCancelButton.setVisibility(View.VISIBLE);
            holder.mTitleTextView.setVisibility(View.INVISIBLE);
            holder.mDescriptionTextView.setVisibility(View.INVISIBLE);
            holder.mNoteButton.setVisibility(View.INVISIBLE);
            holder.mThumbnailImageView.setVisibility(View.INVISIBLE);
            holder.itemView.setBackgroundColor(Color.RED);

        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.mDeleteButton.setVisibility(View.GONE);
            holder.mCancelButton.setVisibility(View.GONE);
            holder.mTitleTextView.setVisibility(View.VISIBLE);
            holder.mDescriptionTextView.setVisibility(View.VISIBLE);
            holder.mNoteButton.setVisibility(View.VISIBLE);
            holder.mThumbnailImageView.setVisibility(View.VISIBLE);
        }

        holder.mReport = report;
        holder.mTitleTextView.setText(report.getTitle());
        holder.mDescriptionTextView.setText(report.getDescription());

        Drawable thumbnail = ReportManager.getInstance().thumbnailForReport(report);
        holder.mThumbnailImageView.setImageDrawable(thumbnail);

        holder.itemView.setEnabled(holder.mReport != null && holder.mReport.isEnabled());
        if (holder.itemView.isEnabled()) {
            holder.mNoteButton.setVisibility(View.VISIBLE);
        }
        else {
            holder.mNoteButton.setVisibility(View.INVISIBLE);
        }
    }

    public void swiped(int swipedPosition, Activity activity) {
        this.swipedReports.add(reports.get(swipedPosition));
        notifyItemChanged(swipedPosition);
        activityToUpdate = activity;
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Report mReport;
        private ImageView mThumbnailImageView;
        private TextView mTitleTextView;
        private TextView mDescriptionTextView;
        private Button mNoteButton;
        private Button mDeleteButton;
        private Button mCancelButton;

        public ViewHolder(View v) {
            super(v);

            mThumbnailImageView = (ImageView) v.findViewById(R.id.card_thumbnail);
            mTitleTextView = (TextView) v.findViewById(R.id.card_title);
            mDescriptionTextView = (TextView) v.findViewById(R.id.card_description);
            mNoteButton = (Button) v.findViewById(R.id.view_note_button);
            mNoteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent noteIntent = new Intent(context, NoteActivity.class);
                    noteIntent.putExtra("report", mReport);
                    context.startActivity(noteIntent);
                }
            });

            mDeleteButton = (Button) v.findViewById(R.id.delete_report_button);
            mDeleteButton.setOnClickListener(new View.OnClickListener() {
               public void onClick(View view) {
                   ReportManager.getInstance().deleteReport(mReport);
                   ReportManager.getInstance().refreshReports(activityToUpdate);
               }
            });

            mCancelButton = (Button) v.findViewById(R.id.cancel_delete_button);
            mCancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // let the card go back to how it was
                    swipedReports.remove(mReport);
                    notifyItemChanged(reports.indexOf(mReport));
                }
            });

            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (!mReport.isEnabled()) {
                return;
            }

            if (mReport.getId() != null && mReport.getId().equalsIgnoreCase(ReportManager.USER_GUIDE_REPORT_ID)) {
                String url = "https://github.com/ngageoint/disconnected-content-explorer-examples/raw/master/reportzips/DICEUserGuide.zip";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                context.startActivity(i);
            } else {
                callbacks.reportSelectedToView(mReport);
            }
        }
    }
}
