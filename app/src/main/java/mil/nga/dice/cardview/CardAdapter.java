package mil.nga.dice.cardview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private List<Report> reports;
    private static LayoutInflater inflater = null;
    private static Activity activity;

    public CardAdapter(Activity activity, List<Report> reports) {
        this.reports = reports;
        this.activity = activity;
        inflater = (LayoutInflater) this.activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

        holder.mReport = report;
        holder.mTitleTextView.setText(report.getTitle());
        holder.mDescriptionTextView.setText(report.getDescription());

        Bitmap bitmap = null;
        if (report.getThumbnail() != null) {
            File image = new File(report.getPath(), report.getThumbnail());
            if (image.exists()) {
                bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
            }
        }
        holder.mThumbnailImageView.setImageBitmap(bitmap);

        holder.itemView.setEnabled(holder.mReport != null && holder.mReport.isEnabled());
    }


    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public Report mReport;
        public ImageView mThumbnailImageView;
        public TextView mTitleTextView;
        public TextView mDescriptionTextView;

        public ViewHolder(View v) {
            super(v);
            mThumbnailImageView = (ImageView) v.findViewById(R.id.card_thumbnail);
            mTitleTextView = (TextView) v.findViewById(R.id.card_title);
            mDescriptionTextView = (TextView) v.findViewById(R.id.card_description);
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (!mReport.isEnabled()) {
                return;
            }
            Intent detailIntent = new Intent(activity, ReportDetailActivity.class);
            detailIntent.putExtra("report", mReport);
            activity.startActivity(detailIntent);
        }
    }
}
