package mil.nga.dice.cardview;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionCallbacks;
import mil.nga.dice.report.NoteActivity;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportManager;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private final Context context;
    private final List<Report> reports;
    private final ReportCollectionCallbacks callbacks;
    private final LayoutInflater inflater;

    public CardAdapter(Context context, List<Report> reports, ReportCollectionCallbacks callbacks) {
        this.reports = reports;
        this.context = context;
        this.callbacks = callbacks;
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

        if (!ReportManager.getInstance().reportHasNote(report)) {
            holder.mNoteButton.setVisibility(View.INVISIBLE);
        }
        else {
            holder.mNoteButton.setVisibility(View.VISIBLE);
        }

        holder.itemView.setEnabled(holder.mReport != null && holder.mReport.isEnabled());
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Report mReport;
        private ImageView mThumbnailImageView;
        private TextView mTitleTextView;
        private TextView mDescriptionTextView;
        private Button mNoteButton;

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
