package mil.nga.dice.map;

import java.util.Collection;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Geometry;

public class ReportMapFragment extends Fragment implements OnMapClickListener, OnMarkerClickListener, OnMapLongClickListener, OfflineMapLoader.OnOfflineFeaturesListener {

	private List<Report> mReports;
	ReportManager mReportManger;
	private MapView mMapView;
	private GoogleMap mMap;
	private OfflineMap mOfflineMap;
	private OfflineMapLoader mOfflineMapLoader;
	
	private static final String TAG = "ReportMapFragment";
	
	public ReportMapFragment() {} 
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mReportManger = ReportManager.getInstance();
		mReports = mReportManger.getReports();
		mOfflineMapLoader = new OfflineMapLoader(getActivity().getApplicationContext());
		mOfflineMapLoader.registerOfflineMapListener(this);
		
	}

	
	@Override 
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.activity_report_map, container, false);
		
		mMapView = (MapView) view.findViewById(R.id.map);
		mMapView.onCreate(savedInstanceState);
		
		MapsInitializer.initialize(getActivity().getApplicationContext());
		refreshMapMarkers();
		
		return view;
	}
	
	
	public void refreshMapMarkers () {
		
		if (mMap != null) {
			mMap.clear();
			
			for (int i = 0; i < mReports.size(); i ++) {
				Report report = mReports.get(i);
				if (report.getLat() != null && report.getLon() != null) {
					MarkerOptions marker = new MarkerOptions().position(new LatLng(report.getLat(), report.getLon())).title(report.getTitle());
					marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
					mMap.addMarker(marker);
					
					LatLng latLng = new LatLng(report.getLat(), report.getLon());
					float zoom = mMap.getCameraPosition().zoom < 1 ? 1 : mMap.getCameraPosition().zoom;
					mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {
						@Override
						public void onCancel() {
							// TODO
						}

						@Override
						public void onFinish() {
							// TODO
						}
					});
				}
			}
		}
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		mMapView.onResume();
		
		if (mMap == null) {
			mMap = mMapView.getMap();
			mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			
			LatLng latLng = new LatLng(0.0, 0.0);
			float zoom = mMap.getCameraPosition().zoom < 1 ? 1 : mMap.getCameraPosition().zoom;
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), new CancelableCallback() {
				@Override
				public void onCancel() {
					// TODO
				}

				@Override
				public void onFinish() {
					// TODO
				}
			});
			
			mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {
	            @Override
	            public void onInfoWindowClick(Marker marker) {
	            	
	            	String title = marker.getTitle();
	            	Report report = null;
	            	
	            	for (int i = 0; i < mReports.size(); i ++) {
	    				if (mReports.get(i).getTitle().equalsIgnoreCase(title)) {
	    					report = mReports.get(i);
	    					break;
	    				}
	            	}
	            	
	            	Intent detailIntent = new Intent(getActivity(), ReportDetailActivity.class);
	    			detailIntent.putExtra("report", report);
	    			startActivity(detailIntent);
	            }
	        });
		}
		
		mMap.setOnMapClickListener(this);
		mMap.setOnMarkerClickListener(this);
		mMap.setOnMapLongClickListener(this);

		refreshMapMarkers();
	}

	
	@Override
	public void onPause () {
		super.onPause();
		mMapView.onPause();
	}

	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mMapView.onLowMemory();
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
	public void onOfflineFeaturesLoaded(Collection<Geometry> offlineFeatures) {
		mOfflineMap = new OfflineMap(getActivity().getApplicationContext(), mMap, offlineFeatures);
		mOfflineMap.setVisible(true);
	}
}
