package mil.nga.dice.listview;

import java.io.File;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.dice.report.Report;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomList extends BaseAdapter {
	
	private List<Report> reports;
	private static LayoutInflater inflater = null;
	
	public CustomList(Activity activity, List<Report> reports) {
		this.reports = reports;
		inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	
	public int getCount () {
		return reports.size();
	}
	
	
	public Object getItem(int position) {
		return reports.get(position);
	}
	
	
	public long getItemId(int position) {
		return position;
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if (convertView == null) {
			view = inflater.inflate(R.layout.list_row, null);
		}
		
		TextView title = (TextView)view.findViewById(R.id.title);
		TextView classification = (TextView)view.findViewById(R.id.classification);
		ImageView thumbnail = (ImageView)view.findViewById(R.id.list_image);
		
		Report report = reports.get(position);
		title.setText(report.getTitle());
		classification.setText(report.getDescription());
		
		if (!report.isEnabled()) {
			title.setTextColor(Color.rgb(150,150,150));
			classification.setTextColor(Color.rgb(150,150,150));
		}
		else {
			title.setTextColor(Color.rgb(0,0,0));
			classification.setTextColor(Color.rgb(0,0,0));
		}

		if (report.getThumbnail() != null) {
			File image = new File(report.getPath(), report.getThumbnail());
			if (image.exists()) {
				Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
				thumbnail.setImageBitmap(bitmap);
			}
		}
		
		view.setEnabled(report.isEnabled());
		
		return view;
	}
}
