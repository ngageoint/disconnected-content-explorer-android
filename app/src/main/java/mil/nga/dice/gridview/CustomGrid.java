package mil.nga.dice.gridview;

import java.io.File;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.dice.report.Report;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomGrid extends BaseAdapter {

	private final Activity mActivity;
	private List<Report> mReports;
	private static LayoutInflater inflater = null;
	
	public CustomGrid(Activity activity, List<Report> reports) {
		this.mActivity = activity;
		this.mReports = reports;
		inflater = (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	
	@Override
	public int getCount() {
		return mReports.size();
	}

	
	@Override
	public Object getItem(int position) {
		return mReports.get(position);
	}

	
	@Override
	public long getItemId(int position) {
		return position;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		
		if (convertView != null) {
			 view = convertView;
		} else {
			view = inflater.inflate(R.layout.grid_item, null, false);
		}
		
		ImageView tileThumbnail = (ImageView)view.findViewById(R.id.tileThumbnail);
		TextView title = (TextView)view.findViewById(R.id.title);

		Report report = mReports.get(position);
		title.setText(report.getTitle());
		
		File image = new File(report.getPath() + "/" + report.getThumbnail());
		if (image.exists()) {
			Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
			tileThumbnail.setImageBitmap(bitmap);
		}
		
		return view;
	}
}
