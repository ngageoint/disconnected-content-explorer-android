package mil.nga.dice.map;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;

public class ReportMapFragment extends Fragment implements
        OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener,
        OnMapLongClickListener, OnInfoWindowClickListener {


	private static final String TAG = "ReportMap";


    private List<Report> reports;
    private List<Marker> reportMarkers;
    private MapView mapView;
    private GoogleMap map;
    private OfflineMap offlineMap;


	public ReportMapFragment() {}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		reports = ReportManager.getInstance().getReports();

		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		bm.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				refreshMapMarkers();
			}
		}, new IntentFilter(ReportManager.INTENT_UPDATE_REPORT_LIST));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_report_map, container, false);
		
		mapView = (MapView) view.findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
		
		return view;
	}
	
	private void refreshMapMarkers () {
		if (map == null) {
			return;
		}

        if (reportMarkers != null) {
            Iterator<Marker> markers = this.reportMarkers.iterator();
            while (markers.hasNext()) {
                Marker marker = markers.next();
                markers.remove();
                marker.remove();
            }
        }

        reportMarkers = new ArrayList<>(reports.size());

		for (Report report : reports) {
			if (report.getLat() != null && report.getLon() != null) {
				MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(report.getLat(), report.getLon()))
                        .title(report.getTitle())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
				reportMarkers.add(map.addMarker(marker));
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();

		refreshMapMarkers();
	}

	@Override
	public void onPause () {
		super.onPause();
		mapView.onPause();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mapView.onLowMemory();
        // TODO: clear the offline map and add logic to restore it later
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        offlineMap.clear();
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        String title = marker.getTitle();
        Report targetReport = null, currentReport;
        Iterator<Report> cursor = reports.iterator();
        while (cursor.hasNext() && targetReport == null) {
            currentReport = cursor.next();
            if (currentReport.getTitle().equalsIgnoreCase(title)) {
                targetReport = currentReport;
            }
        }
        Intent detailIntent = new Intent(getActivity(), ReportDetailActivity.class);
        detailIntent.putExtra("report", targetReport);
        startActivity(detailIntent);
    }

	@Override
	public void onMapLongClick(LatLng arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onMarkerClick(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onMapClick(LatLng arg0) {
		// TODO Auto-generated method stub
	}

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        map.setMapType(GoogleMap.MAP_TYPE_NONE);

        map.setOnInfoWindowClickListener(this);
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
        map.setOnMapLongClickListener(this);

        LatLng latLng = new LatLng(0.0, 0.0);
        float zoom = map.getCameraPosition().zoom < 1 ? 1 : map.getCameraPosition().zoom;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {
            @Override
            public void onCancel() {
                // TODO
            }
            @Override
            public void onFinish() {
                // TODO
            }
        });

        offlineMap = new OfflineMap(map);
        offlineMap.setVisible(true);

        refreshMapMarkers();
    }


}
