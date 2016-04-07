package mil.nga.dice.map.geopackage;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.dice.R;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.tiles.overlay.FeatureTableData;

/**
 * Map data managed for a single GeoPackage
 */
public class GeoPackageMapData {

    /**
     * GeoPackage name
     */
    private String name;

    /**
     * Enabled state
     */
    private boolean enabled;

    /**
     * Locked state
     */
    private boolean locked;

    /**
     * GeoPackage tables
     */
    private Map<String, GeoPackageTableMapData> tableData = new HashMap<>();

    /**
     * GeoPackage tables list
     */
    private List<GeoPackageTableMapData> tableDataList = new ArrayList<>();

    /**
     * Constructor
     *
     * @param name GeoPackage name
     */
    public GeoPackageMapData(String name) {
        this.name = name;
    }

    /**
     * Get the GeoPackage name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Is the GeoPackage enabled
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the GeoPackage as enabled or disabled
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Is the GeoPackage a locked file that should not be deleted?
     *
     * @return true if locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Set if the GeoPackage is locked
     *
     * @param locked true if locked
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * Add a table to the GeoPackage
     *
     * @param table GeoPackage table
     */
    public void addTable(GeoPackageTableMapData table) {
        tableData.put(table.getName(), table);
        tableDataList.add(table);
    }

    /**
     * Get the table map data from the table name
     *
     * @param name table name
     * @return table map data
     */
    public GeoPackageTableMapData getTable(String name) {
        return tableData.get(name);
    }

    /**
     * Get all GeoPackage table map data
     *
     * @return list of table map data
     */
    public List<GeoPackageTableMapData> getTables() {
        return tableDataList;
    }

    /**
     * Remove the GeoPackage from the map
     */
    public void removeFromMap() {
        for (GeoPackageTableMapData table : tableDataList) {
            table.removeFromMap();
        }
    }

    /**
     * Query and build a map click location message from GeoPackage
     *
     * @param latLng  click location
     * @param mapView map view
     * @param map     map
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, MapView mapView, GoogleMap map) {

        StringBuilder clickMessage = new StringBuilder();
        for (GeoPackageTableMapData tableMapData : tableDataList) {
            String message = tableMapData.mapClickMessage(latLng, mapView, map);
            if (message != null) {
                if (clickMessage.length() > 0) {
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
     * @param latLng    click location
     * @param zoom      zoom level
     * @param mapBounds map bounds
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, double zoom, BoundingBox mapBounds) {

        StringBuilder clickMessage = new StringBuilder();
        for (GeoPackageTableMapData tableMapData : tableDataList) {
            String message = tableMapData.mapClickMessage(latLng, zoom, mapBounds);
            if (message != null) {
                if (clickMessage.length() > 0) {
                    clickMessage.append("\n\n");
                }
                clickMessage.append(message);
            }
        }
        return clickMessage.length() > 0 ? clickMessage.toString() : null;
    }

    /**
     * Query and build map click table data from GeoPackage
     *
     * @param latLng            click location
     * @param mapView           map view
     * @param map               map
     * @param includePoints     true to include point geometries
     * @param includeGeometries true to include all geometries
     * @return click table data
     */
    public Map<String, Object> mapClickTableData(LatLng latLng, MapView mapView, GoogleMap map, boolean includePoints, boolean includeGeometries) {
        Map<String, Object> clickData = new HashMap<>();
        for (GeoPackageTableMapData tableMapData : tableDataList) {
            FeatureTableData tableData = tableMapData.mapClickTableData(latLng, mapView, map);
            if (tableData != null) {
                clickData.put(tableData.getName(), tableData.jsonCompatible(includePoints, includeGeometries));
            }
        }

        return clickData.isEmpty() ? null : clickData;
    }

    /**
     * Query and build map click table data from GeoPackage
     *
     * @param latLng            click location
     * @param zoom              zoom level
     * @param mapBounds         map bounds
     * @param includePoints     true to include point geometries
     * @param includeGeometries true to include all geometries
     * @return click table data
     */
    public Map<String, Object> mapClickTableData(LatLng latLng, double zoom, BoundingBox mapBounds, boolean includePoints, boolean includeGeometries) {
        Map<String, Object> clickData = new HashMap<>();
        for (GeoPackageTableMapData tableMapData : tableDataList) {
            FeatureTableData tableData = tableMapData.mapClickTableData(latLng, zoom, mapBounds);
            if (tableData != null) {
                clickData.put(tableData.getName(), tableData.jsonCompatible(includePoints, includeGeometries));
            }
        }

        return clickData.isEmpty() ? null : clickData;
    }

    /**
     * Get the icon image resource id for a GeoPackage
     *
     * @return image resource id
     */
    public Integer getIconImageResourceId() {
        return R.drawable.ic_geopackage;
    }

}
