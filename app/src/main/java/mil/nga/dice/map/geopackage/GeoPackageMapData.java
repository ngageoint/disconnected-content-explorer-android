package mil.nga.dice.map.geopackage;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.tiles.overlay.FeatureTableData;

/**
 *  Map data managed for a single GeoPackage
 */
public class GeoPackageMapData {

    /**
     * GeoPackage name
     */
    private String name;

    /**
     * GeoPackage tables
     */
    private Map<String, GeoPackageTableMapData> tableData = new HashMap<>();

    /**
     *  Constructor
     *
     *  @param name GeoPackage name
     */
    public GeoPackageMapData(String name){
        this.name = name;
    }

    /**
     * Get the GeoPackage name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     *  Add a table to the GeoPackage
     *
     *  @param table GeoPackage table
     */
    public void addTable(GeoPackageTableMapData table){
        tableData.put(table.getName(), table);
    }

    /**
     *  Get the table map data from the table name
     *
     *  @param name table name
     *
     *  @return table map data
     */
    public GeoPackageTableMapData getTable(String name){
        return tableData.get(name);
    }

    /**
     *  Get all GeoPackage table map data
     *
     *  @return collection of table map data
     */
    public Collection<GeoPackageTableMapData> getTables(){
        return tableData.values();
    }

    /**
     *  Remove the GeoPackage from the map
     */
    public void removeFromMap(){
        for(GeoPackageTableMapData table: tableData.values()){
            table.removeFromMap();
        }
    }

    /**
     * Query and build a map click location message from GeoPackage
     *
     * @param latLng click location
     * @param mapView map view
     * @param map map
     *
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, MapView mapView, GoogleMap map) {

        StringBuilder clickMessage = new StringBuilder();
        for(GeoPackageTableMapData tableMapData: tableData.values()){
            String message = tableMapData.mapClickMessage(latLng, mapView, map);
            if(message != null){
                if(clickMessage.length() > 0){
                    clickMessage.append("\n\n");
                }
                clickMessage.append(message);
            }
        }
        return clickMessage.length() > 0 ? clickMessage.toString() : null;
    }

    /**
     * Query and build a map click location message from GeoPackage
     *
     * @param latLng click location
     * @param zoom zoom level
     * @param mapBounds map bounds
     *
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, double zoom, BoundingBox mapBounds){

        StringBuilder clickMessage = new StringBuilder();
        for(GeoPackageTableMapData tableMapData: tableData.values()){
            String message = tableMapData.mapClickMessage(latLng, zoom, mapBounds);
            if(message != null){
                if(clickMessage.length() > 0){
                    clickMessage.append("\n\n");
                }
                clickMessage.append(message);
            }
        }
        return clickMessage.length() > 0 ? clickMessage.toString() : null;
    }

    /**
     *  Query and build map click table data from GeoPackage
     *
     * @param latLng click location
     * @param mapView map view
     * @param map map
     * @param includePoints true to include point geometries
     * @param includeGeometries true to include all geometries
     *
     *  @return click table data
     */
    public Map<String, Object> mapClickTableData(LatLng latLng, MapView mapView, GoogleMap map, boolean includePoints, boolean includeGeometries) {
        Map<String, Object> clickData = new HashMap<>();
        for(GeoPackageTableMapData tableMapData: tableData.values()){
            FeatureTableData tableData = tableMapData.mapClickTableData(latLng, mapView, map);
            if(tableData != null){
                clickData.put(tableData.getName(), tableData.jsonCompatible(includePoints, includeGeometries));
            }
        }

        return clickData.isEmpty() ? null : clickData;
    }

    /**
     *  Query and build map click table data from GeoPackage
     *
     *  @param latLng click location
     *  @param zoom               zoom level
     *  @param mapBounds          map bounds
     *  @param includePoints true to include point geometries
     *  @param includeGeometries true to include all geometries
     *
     *  @return click table data
     */
    public Map<String, Object> mapClickTableData(LatLng latLng, double zoom, BoundingBox mapBounds, boolean includePoints, boolean includeGeometries) {
        Map<String, Object> clickData = new HashMap<>();
        for(GeoPackageTableMapData tableMapData: tableData.values()){
            FeatureTableData tableData = tableMapData.mapClickTableData(latLng, zoom, mapBounds);
            if(tableData != null){
                clickData.put(tableData.getName(), tableData.jsonCompatible(includePoints, includeGeometries));
            }
        }

        return clickData.isEmpty() ? null : clickData;
    }

}
