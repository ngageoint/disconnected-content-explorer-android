package mil.nga.dice.map.geopackage;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;

import java.util.ArrayList;
import java.util.List;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.geom.map.GoogleMapShape;
import mil.nga.geopackage.projection.Projection;
import mil.nga.geopackage.projection.ProjectionConstants;
import mil.nga.geopackage.projection.ProjectionFactory;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.tiles.overlay.FeatureTableData;

/**
 *  Map data managed for a single GeoPackage table
 */
public class GeoPackageTableMapData {

    /**
     *  Tile overlay added to the map view
     */
    private TileOverlay tileOverlay;

    /**
     *  Feature overlay queries for handling map click queries
     */
    private List<FeatureOverlayQuery> featureOverlayQueries;

    /**
     *  Map shapes added to the map view
     */
    private List<GoogleMapShape> mapShapes;

    /**
     * Table name
     */
    private String name;

    /**
     * Projection
     */
    private Projection projection;

    /**
     *  Constructor
     *
     *  @param name table name
     */
    public GeoPackageTableMapData(String name){
        this.name = name;
        projection = ProjectionFactory.getProjection(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM);
    }

    /**
     * Get the tile overlay
     * @return tile overlay
     */
    public TileOverlay getTileOverlay() {
        return tileOverlay;
    }

    /**
     * Set the tile overlay
     * @param tileOverlay tile overlay
     */
    public void setTileOverlay(TileOverlay tileOverlay) {
        this.tileOverlay = tileOverlay;
    }

    /**
     * Get the feature overlay queries
     * @return feature overlay queries
     */
    public List<FeatureOverlayQuery> getFeatureOverlayQueries() {
        return featureOverlayQueries;
    }

    /**
     * Set the feature overlay queries
     * @param featureOverlayQueries feature overlay queries
     */
    public void setFeatureOverlayQueries(List<FeatureOverlayQuery> featureOverlayQueries) {
        this.featureOverlayQueries = featureOverlayQueries;
    }

    /**
     * Get the map shapes
     * @return map shapes
     */
    public List<GoogleMapShape> getMapShapes() {
        return mapShapes;
    }

    /**
     * Set the map shapes
     * @param mapShapes map shapes
     */
    public void setMapShapes(List<GoogleMapShape> mapShapes) {
        this.mapShapes = mapShapes;
    }

    /**
     * Get the GeoPackage table name
     * @return table name
     */
    public String getName() {
        return name;
    }

    /**
     *  Add a feature overlay query to the table
     *
     *  @param query feature overlay query
     */
    public void addFeatureOverlayQuery(FeatureOverlayQuery query){
        if(featureOverlayQueries == null){
            featureOverlayQueries = new ArrayList<>();
        }
        featureOverlayQueries.add(query);
    }

    /**
     *  Add a map shape to the table
     *
     *  @param shape map shape
     */
    public void addMapShape(GoogleMapShape shape) {
        if (mapShapes == null) {
            mapShapes = new ArrayList<>();
        }
        mapShapes.add(shape);
    }

    /**
     *  Remove the GeoPackage table from the map
     */
    public void removeFromMap(){

        if(tileOverlay != null){
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
     * @param latLng click location
     * @param mapView map view
     * @param map map
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, MapView mapView, GoogleMap map) {
        StringBuilder message = null;

        if(featureOverlayQueries != null){
            for(FeatureOverlayQuery featureOverlayQuery: featureOverlayQueries){
                String overlayMessage = featureOverlayQuery.buildMapClickMessage(latLng, mapView, map, projection);
                if(overlayMessage != null){
                    if(message == null){
                        message = new StringBuilder();
                    }else{
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
     * @param latLng click location
     * @param zoom zoom level
     * @param mapBounds map bounds
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, double zoom, BoundingBox mapBounds){
        StringBuilder message = null;

        if(featureOverlayQueries != null){
            for(FeatureOverlayQuery featureOverlayQuery: featureOverlayQueries){
                String overlayMessage = featureOverlayQuery.buildMapClickMessageWithMapBounds(latLng, zoom, mapBounds, projection);
                if(overlayMessage != null){
                    if(message == null){
                        message = new StringBuilder();
                    }else{
                        message.append("\n\n");
                    }
                    message.append(overlayMessage);
                }
            }
        }

        return message != null ? message.toString() : null;
    }

    /**
     *  Query and build map click table data from GeoPackage table
     *
     * @param latLng click location
     * @param mapView map view
     * @param map map
     *
     *  @return click table data
     */
    public FeatureTableData mapClickTableData(LatLng latLng, MapView mapView, GoogleMap map) {
        FeatureTableData tableData = null;

        if(featureOverlayQueries != null){
            for(FeatureOverlayQuery featureOverlayQuery: featureOverlayQueries){
                tableData = featureOverlayQuery.buildMapClickTableData(latLng, mapView, map, projection);
            }
        }

        return tableData;
    }

    /**
     *  Query and build map click table data from GeoPackage table
     *
     *  @param locationCoordinate click location
     *  @param zoom               zoom level
     *  @param mapBounds          map bounds
     *
     *  @return click table data
     */
    public FeatureTableData mapClickTableData(LatLng latLng, double zoom, BoundingBox mapBounds){
        FeatureTableData tableData = null;

        if(featureOverlayQueries != null){
            for(FeatureOverlayQuery featureOverlayQuery: featureOverlayQueries){
                tableData = featureOverlayQuery.buildMapClickTableDataWithMapBounds(latLng, zoom, mapBounds, projection);
            }
        }

        return tableData;
    }

}
