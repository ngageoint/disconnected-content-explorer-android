package mil.nga.dice.map;

//import android.app.Fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mil.nga.dice.DICEConstants;
import mil.nga.dice.R;
import mil.nga.dice.ReportCollectionActivity;
import mil.nga.dice.map.geopackage.GeoPackageMapOverlays;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportDetailActivity;
import mil.nga.dice.report.ReportManager;

public class ReportMapFragment extends android.support.v4.app.Fragment implements
        OnMapReadyCallback, OnMapClickListener, OnMarkerClickListener,
        OnMapLongClickListener, OnInfoWindowClickListener {

	private static final String TAG = "ReportMap";


    private List<Report> reports;
    private List<Marker> reportMarkers;
    private MapView mapView;
    private GoogleMap map;
    private OfflineMap offlineMap;
    private GeoPackageMapOverlays geoPackageMapOverlays;
    private View mapOverlaysView;
    private SharedPreferences settings;

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

        settings = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        Editor editor = settings.edit();
        editor.putBoolean(DICEConstants.DICE_ZOOM_TO_REPORTS, true);
        editor.commit();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_report_map, container, false);
		
		mapView = (MapView) view.findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        mapOverlaysView = view.findViewById(R.id.mapOverlays);
        ImageButton mapOverlaysButton = (ImageButton) mapOverlaysView
                .findViewById(R.id.mapOverlaysButton);
        mapOverlaysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(getActivity(), OverlaysActivity.class);
                getActivity().startActivityForResult(intent, ReportCollectionActivity.OVERLAYS_ACTIVITY);
            }
        });

		return view;
	}
	
	private void refreshMapMarkers () {
		if (map == null) {
			return;
		}

        // Only show the overlay button if there are GeoPackages
        if(geoPackageMapOverlays.hasGeoPackages()) {
            mapOverlaysView.setVisibility(View.VISIBLE);
        }else{
            mapOverlaysView.setVisibility(View.INVISIBLE);
        }

        if (reportMarkers != null) {
            Iterator<Marker> markers = this.reportMarkers.iterator();
            while (markers.hasNext()) {
                Marker marker = markers.next();
                markers.remove();
                marker.remove();
            }
        }

        boolean zoom = settings.getBoolean(DICEConstants.DICE_ZOOM_TO_REPORTS, true);
        if(zoom){
            Editor editor = settings.edit();
            editor.putBoolean(DICEConstants.DICE_ZOOM_TO_REPORTS, false);
            editor.commit();
        }

        reportMarkers = new ArrayList<>(reports.size());

        LatLngBounds.Builder zoomBounds = null;

		for (Report report : reports) {
			if (report.getLat() != null && report.getLon() != null) {
				MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(report.getLat(), report.getLon()))
                        .title(report.getTitle())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
				reportMarkers.add(map.addMarker(marker));

                if(zoom){
                    if(zoomBounds == null){
                        zoomBounds = new LatLngBounds.Builder();
                    }
                    zoomBounds.include(marker.getPosition());
                }
			}
		}

        // Zoom to the reports
        if(zoomBounds != null){
            View view = getView();
            int minViewLength = Math.min(view.getWidth(), view.getHeight());
            final int padding = (int) Math.floor(minViewLength * 0.1);
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(
                        zoomBounds.build(), padding));
            } catch (Exception e) {
                Log.w(ReportMapFragment.class.getSimpleName(),
                        "Unable to move camera", e);
            }
        }

        geoPackageMapOverlays.updateMap();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	public void onPause () {
        geoPackageMapOverlays.deselectedReport();
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
        Report report = getReport(marker);
        if(report != null) {
            geoPackageMapOverlays.deselectedReport();
            Intent detailIntent = new Intent(getActivity(), ReportDetailActivity.class);
            detailIntent.putExtra("report", report);
            startActivity(detailIntent);
        }
    }

    /**
     * Get the Report from the marker
     * @param marker
     * @return report
     */
    private Report getReport(Marker marker) {
        String title = marker.getTitle();
        Report report = null;
        Iterator<Report> cursor = reports.iterator();
        Report currentReport = null;
        while (cursor.hasNext()) {
            currentReport = cursor.next();
            if (currentReport.getTitle().equalsIgnoreCase(title)) {
                report = currentReport;
                break;
            }
        }
        return report;
    }

	@Override
	public void onMapLongClick(LatLng latLng) {

	}

	@Override
	public boolean onMarkerClick(Marker marker) {
        boolean consumed = false;
        String message = geoPackageMapOverlays.mapClickMessage(marker);
        if(message != null && !message.isEmpty()) {
            consumed = true;
            geoPackageMapOverlays.deselectedReport();
            displayMessage(message);
        }else{
            Report report = getReport(marker);
            if(report != null) {
                geoPackageMapOverlays.selectedReport(report);
            }else{
                geoPackageMapOverlays.deselectedReport();
            }
        }

        return consumed;
	}

	@Override
	public void onMapClick(LatLng latLng) {
        geoPackageMapOverlays.deselectedReport();
        String message = geoPackageMapOverlays.mapClickMessage(latLng);
        displayMessage(message);
    }

    /**
     * Display a message
     * @param message message string
     */
    private void displayMessage(String message){
        if(message != null && !message.isEmpty()){
            new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
        }
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

        geoPackageMapOverlays = new GeoPackageMapOverlays(getActivity(), mapView, map);

        refreshMapMarkers();
    }


}
