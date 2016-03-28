package mil.nga.dice.map.geopackage;

import com.google.android.gms.maps.model.LatLng;

import mil.nga.dice.report.Report;

/**
 * Manages GeoPackage feature and tile overlays, including adding to and removing from the map
 */
public class GeoPackageMapOverlays {

    /**
     * Constructor
     */
    public GeoPackageMapOverlays(/*TODO*/){

    }

    /**
     * Determine if there are any GeoPackages within DICE
     *
     * @return true if GeoPackages exist
     */
    public boolean hasGeoPackages(){
        return false; // TODO
    }

    /**
     * Update the map with selected GeoPackages
     */
    public void updateMap(){
        // TODO
    }

    /**
     * Query and build a map click location message from enabled GeoPackage tables
     * @param latLng click location
     * @return click message
     */
    public String mapClickMessage(LatLng latLng){
        return null; // TODO
    }

    /**
     * Report has been selected on the map
     *
     * @param report selected report
     */
    public void selectedReport(Report report){
        // TODO
    }

    /**
     * Report has been deselected on the map
     *
     * @param report deselected report
     */
    public void deselectedReport(Report report){
        // TODO
    }

}
