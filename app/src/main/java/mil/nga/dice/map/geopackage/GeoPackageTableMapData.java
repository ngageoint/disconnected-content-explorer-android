package mil.nga.dice.map.geopackage;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.ArrayList;
import java.util.List;

import mil.nga.dice.R;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.projection.Projection;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.tiles.overlay.FeatureTableData;

/**
 * Map data managed for a single GeoPackage table
 */
public class GeoPackageTableMapData {

    /**
     * Tile overlay added to the map view
     */
    private TileOverlay tileOverlay;

    /**
     * Feature overlay queries for handling map click queries
     */
    private List<FeatureOverlayQuery> featureOverlayQueries;

    /**
     * Map shapes added to the map view
     */
    private List<GoogleMapShape> mapShapes = new ArrayList<>();

    /**
     * Table name
     */
    private String name;

    /**
     * Enabled state
     */
    private boolean enabled;

    /**
     * Projection
     */
    private Projection projection;

    /**
     * True if feature table, false if tile table
     */
    private boolean featureTable;

    /**
     * Table count
     */
    private int count;

    /**
     * Min zoom level
     */
    private long minZoom;

    /**
     * Max zoom level
     */
    private long maxZoom;

    /**
     * Linked table map data
     */
    private List<GeoPackageTableMapData> linked = new ArrayList<>();

    /**
     * Constructor
     *
     * @param name         table name
     * @param featureTable true for feature table, false for tile table
     */
    public GeoPackageTableMapData(String name, boolean featureTable) {
        this.name = name;
        projection = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);
        this.featureTable = featureTable;
    }

    /**
     * Get the tile overlay
     *
     * @return tile overlay
     */
    public TileOverlay getTileOverlay() {
        return tileOverlay;
    }

    /**
     * Set the tile overlay
     *
     * @param tileOverlay tile overlay
     */
    public void setTileOverlay(TileOverlay tileOverlay) {
        this.tileOverlay = tileOverlay;
    }

    /**
     * Get the feature overlay queries
     *
     * @return feature overlay queries
     */
    public List<FeatureOverlayQuery> getFeatureOverlayQueries() {
        return featureOverlayQueries;
    }

    /**
     * Set the feature overlay queries
     *
     * @param featureOverlayQueries feature overlay queries
     */
    public void setFeatureOverlayQueries(List<FeatureOverlayQuery> featureOverlayQueries) {
        this.featureOverlayQueries = featureOverlayQueries;
    }

    /**
     * Get the map shapes
     *
     * @return map shapes
     */
    public List<GoogleMapShape> getMapShapes() {
        return mapShapes;
    }

    /**
     * Set the map shapes
     *
     * @param mapShapes map shapes
     */
    public void setMapShapes(List<GoogleMapShape> mapShapes) {
        this.mapShapes = mapShapes;
    }

    /**
     * Get the GeoPackage table name
     *
     * @return table name
     */
    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for(GeoPackageTableMapData linkedTable: linked){
            linkedTable.setEnabled(enabled);
        }
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(long minZoom) {
        this.minZoom = minZoom;
    }

    public long getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(long maxZoom) {
        this.maxZoom = maxZoom;
    }

    public List<GeoPackageTableMapData> getLinked() {
        return linked;
    }

    public void addLinked(GeoPackageTableMapData link){
        linked.add(link);
    }

    /**
     * Add a feature overlay query to the table
     *
     * @param query feature overlay query
     */
    public void addFeatureOverlayQuery(FeatureOverlayQuery query) {
        if (featureOverlayQueries == null) {
            featureOverlayQueries = new ArrayList<>();
        }
        featureOverlayQueries.add(query);
    }

    /**
     * Add a map shape to the table
     *
     * @param shape map shape
     */
    public void addMapShape(GoogleMapShape shape) {
        mapShapes.add(shape);
    }

    /**
     * Remove the GeoPackage table from the map
     */
    public void removeFromMap() {

        if (tileOverlay != null) {
            tileOverlay.remove();
            tileOverlay = null;
        }

        for (GoogleMapShape shape : mapShapes) {
            shape.remove();
        }
        mapShapes.clear();
    }

    /**
     * Query and build a map click location message from GeoPackage table
     *
     * @param latLng  click location
     * @param mapView map view
     * @param map     map
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, MapView mapView, GoogleMap map) {
        StringBuilder message = null;

        if (featureOverlayQueries != null) {
            for (FeatureOverlayQuery featureOverlayQuery : featureOverlayQueries) {
                String overlayMessage = featureOverlayQuery.buildMapClickMessage(latLng, mapView, map, projection);
                if (overlayMessage != null) {
                    if (message == null) {
                        message = new StringBuilder();
                    } else {
                        message.append("\n\n");
                    }
                    message.append(overlayMessage);
                }
            }
        }

        return message != null ? message.toString() : null;
    }

    /**
     * Query and build a map click location message from GeoPackage table
     *
     * @param latLng    click location
     * @param zoom      zoom level
     * @param mapBounds map bounds
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, double zoom, BoundingBox mapBounds) {
        StringBuilder message = null;

        if (featureOverlayQueries != null) {
            for (FeatureOverlayQuery featureOverlayQuery : featureOverlayQueries) {
                String overlayMessage = featureOverlayQuery.buildMapClickMessageWithMapBounds(latLng, zoom, mapBounds, projection);
                if (overlayMessage != null) {
                    if (message == null) {
                        message = new StringBuilder();
                    } else {
                        message.append("\n\n");
                    }
                    message.append(overlayMessage);
                }
            }
        }

        return message != null ? message.toString() : null;
    }

    /**
     * Query and build map click table data from GeoPackage table
     *
     * @param latLng  click location
     * @param mapView map view
     * @param map     map
     * @return click table data
     */
    public FeatureTableData mapClickTableData(LatLng latLng, MapView mapView, GoogleMap map) {
        FeatureTableData tableData = null;

        if (featureOverlayQueries != null) {
            for (FeatureOverlayQuery featureOverlayQuery : featureOverlayQueries) {
                tableData = featureOverlayQuery.buildMapClickTableData(latLng, mapView, map, projection);
            }
        }

        return tableData;
    }

    /**
     * Query and build map click table data from GeoPackage table
     *
     * @param latLng    click location
     * @param zoom      zoom level
     * @param mapBounds map bounds
     * @return click table data
     */
    public FeatureTableData mapClickTableData(LatLng latLng, double zoom, BoundingBox mapBounds) {
        FeatureTableData tableData = null;

        if (featureOverlayQueries != null) {
            for (FeatureOverlayQuery featureOverlayQuery : featureOverlayQueries) {
                tableData = featureOverlayQuery.buildMapClickTableDataWithMapBounds(latLng, zoom, mapBounds, projection);
            }
        }

        return tableData;
    }

    public Integer getIconImageResourceId() {
        Integer icon = null;
        if (featureTable) {
            icon = R.drawable.ic_place;
        } else {
            icon = R.drawable.ic_tiles;
        }
        return icon;
    }

    public String getInfo() {
        String info = null;
        if (featureTable) {
            info = "features";
        } else {
            info = "tiles";
        }
        long min = minZoom;
        long max = maxZoom;
        for(GeoPackageTableMapData linkedTable: linked){
            min = Math.min(min, linkedTable.getMinZoom());
            max = Math.max(max, linkedTable.getMaxZoom());
        }
        info += ": " + count + ", zoom: " + min + " - " + max;
        return info;
    }

}
