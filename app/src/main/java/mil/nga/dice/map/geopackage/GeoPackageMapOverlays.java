package mil.nga.dice.map.geopackage;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mil.nga.dice.DICEConstants;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportCache;
import mil.nga.geopackage.GeoPackageCache;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;

/**
 * Manages GeoPackage feature and tile overlays, including adding to and removing from the map
 */
public class GeoPackageMapOverlays {

    /**
     * Map View
     */
    private final MapView mapView;

    /**
     * Google Map
     */
    private final GoogleMap map;

    /**
     * GeoPackage manager
     */
    private final GeoPackageManager manager;

    /**
     * GeoPackage cache
     */
    private final GeoPackageCache cache;

    /**
     * Mapping between GeoPackage name and the map data
     */
    private Map<String, GeoPackageMapData> mapData = new HashMap<>();

    /**
     * Current map selected report
     */
    private Report selectedReport;

    /**
     * Synchronizing lock
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Constructor
     */
    public GeoPackageMapOverlays(Context context, MapView mapView, GoogleMap map) {
        this.mapView = mapView;
        this.map = map;
        manager = GeoPackageFactory.getManager(context);
        cache = new GeoPackageCache(manager);
    }

    /**
     * Determine if there are any GeoPackages within DICE
     *
     * @return true if GeoPackages exist
     */
    public boolean hasGeoPackages() {
        String like = DICEConstants.DICE_TEMP_CACHE_SUFFIX + "%";
        List<String> geoPackages = null;
        try {
            geoPackages = manager.databases(); // TODO not like query
        } catch (Exception e) {
            Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to find shared GeoPackage count", e);
        }
        return geoPackages != null && !geoPackages.isEmpty();
    }

    /**
     * Update the map with selected GeoPackages
     */
    public void updateMap() {
        lock.lock();
        try {
            updateMapSynchronized();
        } catch (Exception e) {
            Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to update map with active GeoPackages", e);
        } finally {
            lock.unlock();
        }
    }

    public void updateMapSynchronized() {
        // TODO
    }

    /**
     * Query and build a map click location message from enabled GeoPackage tables
     *
     * @param latLng click location
     * @return click message
     */
    public String mapClickMessage(LatLng latLng) {
        StringBuilder clickMessage = new StringBuilder();
        if(selectedReport != null){
            for(GeoPackageMapData data: mapData.values()){
                String  message = data.mapClickMessage(latLng, mapView, map);
                if(message != null){
                    if(clickMessage.length() > 0){
                        clickMessage.append("\n\n");
                    }
                    clickMessage.append(message);
                }
            }
        }

        return clickMessage.length() > 0 ? clickMessage.toString() : null;
    }

    /**
     * Report has been selected on the map
     *
     * @param report selected report
     */
    public void selectedReport(Report report) {

        if(!report.getCacheFiles().isEmpty()){

            for(ReportCache reportCache: report.getCacheFiles()){

                boolean exists = false;
                try{
                    exists = manager.exists(reportCache.getName());
                }catch(Exception e){
                    Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to check if GeoPackage exists"  + reportCache.getName(), e);
                }

                if(!exists){
                    try{
                        manager.importGeoPackageAsExternalLink(reportCache.getPath(), reportCache.getName(), true);
                    }catch(Exception e){
                        Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to import GeoPackage " + reportCache.getName() + " at path: " + reportCache.getPath(), e);
                    }
                }
            }

            selectedReport = report;

            //updateSelectedCaches(); TODO
        }
    }

    /**
     * Report has been deselected on the map
     *
     * @param report deselected report
     */
    public void deselectedReport(Report report) {

        boolean change = false;

        selectedReport = null;

        String like = DICEConstants.DICE_TEMP_CACHE_SUFFIX + "%";
        List<String> geoPackages = null;
        try{
            geoPackages = manager.databases(); // TODO
        }catch(Exception e){
            Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to find temporary GeoPackages", e);
        }
        if(geoPackages != null){
            for(String geoPackage: geoPackages){
                cache.close(geoPackage);
                try{
                    manager.delete(geoPackage);
                }catch(Exception e){
                    Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to delete GeoPackage: " + geoPackage, e);
                }
                change = true;
            }
        }

        if(change){
            //updateSelectedCaches(); TODO
        }
    }

}
