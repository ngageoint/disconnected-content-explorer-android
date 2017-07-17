package mil.nga.dice.map.geopackage;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import mil.nga.dice.DICEConstants;
import mil.nga.dice.report.Report;
import mil.nga.dice.report.ReportCache;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageCache;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShape;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.geopackage.map.geom.GoogleMapShapeType;
import mil.nga.geopackage.projection.Projection;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.DefaultFeatureTiles;
import mil.nga.geopackage.tiles.features.custom.NumberFeaturesTile;
import mil.nga.geopackage.map.tiles.overlay.BoundedOverlay;
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlay;
import mil.nga.geopackage.map.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.map.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.wkb.geom.Geometry;
import mil.nga.wkb.geom.GeometryType;
import mil.nga.wkb.util.GeometryPrinter;

/**
 * Manages GeoPackage feature and tile overlays, including adding to and removing from the map
 */
public class GeoPackageMapOverlays {

    /**
     * Context
     */
    private final Context context;

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
     * GeoPackage list
     */
    private List<GeoPackageMapData> mapDataList = new ArrayList<>();

    /**
     * Current map selected report
     */
    private Report selectedReport;

    /**
     * Synchronizing lock
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Selected GeoPackage settings
     */
    private GeoPackageSelected selectedSettings;

    /**
     * Marker feature
     */
    class MarkerFeature {
        long featureId;
        String database;
        String tableName;
    }

    /**
     * Mapping between marker ids and the features
     */
    private Map<String, MarkerFeature> markerIds = new HashMap<String, MarkerFeature>();

    /**
     * Constructor
     *
     * @param context
     * @param map
     * @param mapView
     */
    public GeoPackageMapOverlays(Context context, MapView mapView, GoogleMap map) {
        this.context = context;
        this.mapView = mapView;
        this.map = map;
        manager = GeoPackageFactory.getManager(context);
        cache = new GeoPackageCache(manager);
        selectedSettings = new GeoPackageSelected(context);
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
            geoPackages = manager.databasesNotLike(like);
        } catch (Exception e) {
            Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to find shared GeoPackage count", e);
        }
        return geoPackages != null && !geoPackages.isEmpty();
    }

    /**
     * Get the map data
     *
     * @return map data
     */
    public Map<String, GeoPackageMapData> getMapData() {
        return mapData;
    }

    /**
     * Get the map data list
     *
     * @return map data list
     */
    public List<GeoPackageMapData> getMapDataList() {
        return mapDataList;
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

    /**
     * Update the map while synchronized
     */
    private void updateMapSynchronized() {

        Map<String, Set<String>> selectedCaches = selectedSettings.getSelectedMap();
        Map<String, Set<String>> updateSelectedCaches = new HashMap<>(selectedCaches);

        Set<String> selectedGeoPackages = new HashSet<>();
        if (selectedReport != null) {
            for (ReportCache reportCache : selectedReport.getCacheFiles()) {
                updateSelectedCaches.put(reportCache.getName(), new HashSet<String>());
                selectedGeoPackages.add(reportCache.getName());
            }
        }

        String like = DICEConstants.DICE_TEMP_CACHE_SUFFIX + "%";
        List<String> geoPackages = null;
        try {
            geoPackages = manager.databasesLike(like);
        } catch (Exception e) {
            Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to find temporary GeoPackages", e);
        }
        if (geoPackages != null) {
            for (String geoPackage : geoPackages) {
                if (!selectedGeoPackages.contains(geoPackage)) {
                    cache.close(geoPackage);
                    try {
                        manager.delete(geoPackage);
                    } catch (Exception e) {
                        Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to delete GeoPackage: " + geoPackage, e);
                    }
                }
            }
        }

        Map<String, GeoPackageMapData> newMapData = new HashMap<>();

        // Add the GeoPackage caches
        for (String name : updateSelectedCaches.keySet()) {

            boolean deleteFromSelected = true;

            // Make sure the GeoPackage exists

            boolean exists = false;
            try {
                exists = manager.exists(name);
            } catch (Exception e) {
                Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to check if GeoPackage exists: " + name, e);
            }

            if (exists) {

                // Make sure the GeoPackage file exists
                File file = null;
                try {
                    file = manager.getFile(name);
                } catch (Exception e) {
                    Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to get file for GeoPackage: " + name, e);
                }

                if (file != null) {

                    deleteFromSelected = false;

                    Set<String> selected = updateSelectedCaches.get(name);

                    // Close a previously open GeoPackage connection if a new GeoPackge version
                    boolean removeExistingFromMap = false;
                    if (selected.isEmpty()) {
                        cache.close(name);
                        removeExistingFromMap = true;
                    }

                    GeoPackage geoPackage = cache.getOrOpen(name);

                    GeoPackageMapData existingGeoPackageData = mapData.get(name);

                    // If the GeoPackage is selected with no tables, select all of them as it is a new version
                    if (selected.isEmpty()) {
                        selected.addAll(geoPackage.getTables());
                        if (!selectedGeoPackages.contains(name)) {
                            updateSelectedCaches.put(name, selected);
                            selectedSettings.updateSelected(updateSelectedCaches);
                            removeExistingFromMap = true;
                        }
                    }

                    if(removeExistingFromMap && existingGeoPackageData != null){
                        existingGeoPackageData.removeFromMap(markerIds);
                        existingGeoPackageData = null;
                    }

                    GeoPackageMapData geoPackageData = new GeoPackageMapData(name);
                    newMapData.put(geoPackageData.getName(), geoPackageData);

                    addGeoPackage(geoPackage, selected, geoPackageData, existingGeoPackageData);
                } else {
                    // Delete if the file was deleted
                    try {
                        manager.delete(name);
                    } catch (Exception e) {
                        Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to delete GeoPackage: " + name, e);
                    }
                }
            }

            // Remove the GeoPackage from the list of selected
            if (deleteFromSelected) {
                updateSelectedCaches.remove(name);
                selectedSettings.updateSelected(updateSelectedCaches);
            }
        }

        // Remove GeoPackage tables from the map that are no longer selected
        for (GeoPackageMapData oldGeoPackageMapData : mapDataList) {

            GeoPackageMapData newGeoPackageMapData = newMapData.get(oldGeoPackageMapData.getName());
            if (newGeoPackageMapData == null) {
                oldGeoPackageMapData.removeFromMap(markerIds);
                cache.close(oldGeoPackageMapData.getName());
            } else {

                for (GeoPackageTableMapData oldGeoPackageTableMapData : oldGeoPackageMapData.getTables()) {

                    GeoPackageTableMapData newGeoPackageTableMapData = newGeoPackageMapData.getTable(oldGeoPackageTableMapData.getName());

                    if (newGeoPackageTableMapData == null) {
                        oldGeoPackageTableMapData.removeFromMap(markerIds);
                    }
                }
            }
        }

        mapData = newMapData;
        mapDataList = new ArrayList<>(mapData.values());
    }

    /**
     * Add the GeoPackage to the map
     *
     * @param geoPackage
     * @param selected
     * @param data
     * @param existingData
     */
    private void addGeoPackage(GeoPackage geoPackage, Set<String> selected, GeoPackageMapData data, GeoPackageMapData existingData) {

        for (String table : selected) {

            boolean addNew = true;

            if (existingData != null) {
                GeoPackageTableMapData tableData = existingData.getTable(table);
                if (tableData != null) {
                    addNew = false;
                    data.addTable(tableData);
                }
            }

            if (addNew) {
                try {
                    if (geoPackage.isTileTable(table)) {
                        addTileTable(geoPackage, table, data);
                    } else if (geoPackage.isFeatureTable(table)) {
                        addFeatureTable(geoPackage, table, data);
                    }
                }catch(Exception e){
                    Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to add table: " + table + ", GeoPackage: " + geoPackage.getName(), e);
                }
            }
        }
    }

    /**
     * Add a tile table to the map
     *
     * @param geoPackage
     * @param name
     * @param data
     */
    private void addTileTable(GeoPackage geoPackage, String name, GeoPackageMapData data) {

        GeoPackageTableMapData tableData = new GeoPackageTableMapData(name, false);
        data.addTable(tableData);

        // Create a new GeoPackage tile provider and add to the map
        TileDao tileDao = geoPackage.getTileDao(name);
        BoundedOverlay geoPackageTileOverlay = GeoPackageOverlayFactory.getBoundedOverlay(tileDao);

        // Check for linked feature tables
        FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
        List<FeatureDao> featureDaos = linker.getFeatureDaosForTileTable(tileDao.getTableName());
        for (FeatureDao featureDao : featureDaos) {

            // Create the feature tiles
            FeatureTiles featureTiles = new DefaultFeatureTiles(context, featureDao);

            // Create an index manager
            FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
            featureTiles.setIndexManager(indexer);

            // Add the feature overlay query
            FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(context, geoPackageTileOverlay, featureTiles);
            tableData.addFeatureOverlayQuery(featureOverlayQuery);
        }

        // Set the tiles index to be -2 of it is behind features and tiles drawn from features
        int zIndex = -2;

        // If these tiles are linked to features, set the zIndex to -1 so they are placed before imagery tiles
        if (!featureDaos.isEmpty()) {
            zIndex = -1;
        }

        TileOverlayOptions overlayOptions = createTileOverlayOptions(geoPackageTileOverlay, zIndex);
        TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
        tableData.setTileOverlay(tileOverlay);
    }

    /**
     * Add a feature table to the map
     *
     * @param geoPackage
     * @param name
     * @param data
     */
    private void addFeatureTable(GeoPackage geoPackage, String name, GeoPackageMapData data) {

        GeoPackageTableMapData tableData = new GeoPackageTableMapData(name, true);
        data.addTable(tableData);

        // Create a new GeoPackage tile provider and add to the map
        FeatureDao featureDao = geoPackage.getFeatureDao(name);

        FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);

        if (indexer.isIndexed()) {
            FeatureTiles featureTiles = new DefaultFeatureTiles(context, featureDao);
            Integer maxFeaturesPerTile = null;
            if (featureDao.getGeometryType() == GeometryType.POINT) {
                maxFeaturesPerTile = DICEConstants.DICE_CACHE_FEATURE_TILES_MAX_POINTS_PER_TILE;
            } else {
                maxFeaturesPerTile = DICEConstants.DICE_CACHE_FEATURE_TILES_MAX_FEATURES_PER_TILE;
            }
            featureTiles.setMaxFeaturesPerTile(maxFeaturesPerTile);
            NumberFeaturesTile numberFeaturesTile = new NumberFeaturesTile(context);
            // Adjust the max features number tile draw paint attributes here as needed to
            // change how tiles are drawn when more than the max features exist in a tile
            featureTiles.setMaxFeaturesTileDraw(numberFeaturesTile);
            featureTiles.setIndexManager(indexer);
            // Adjust the feature tiles draw paint attributes here as needed to change how
            // features are drawn on tiles
            FeatureOverlay featureOverlay = new FeatureOverlay(featureTiles);
            featureOverlay.setMinZoom(featureDao.getZoomLevel());

            FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
            List<TileDao> tileDaos = linker.getTileDaosForFeatureTable(featureDao.getTableName());
            featureOverlay.ignoreTileDaos(tileDaos);

            FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(context, featureOverlay);
            tableData.addFeatureOverlayQuery(featureOverlayQuery);

            TileOverlayOptions overlayOptions = createTileOverlayOptions(featureOverlay, -1);
            TileOverlay tileOverlay = map.addTileOverlay(overlayOptions);
            tableData.setTileOverlay(tileOverlay);
        } else {
            indexer.close();
            int maxFeaturesPerTable = 0;
            if (featureDao.getGeometryType() == GeometryType.POINT) {
                maxFeaturesPerTable = DICEConstants.DICE_CACHE_FEATURES_MAX_POINTS_PER_TABLE;
            } else {
                maxFeaturesPerTable = DICEConstants.DICE_CACHE_FEATURES_MAX_FEATURES_PER_TABLE;
            }
            Projection projection = featureDao.getProjection();
            GoogleMapShapeConverter shapeConverter = new GoogleMapShapeConverter(projection);
            FeatureCursor featureCursor = featureDao.queryForAll();
            try {
                final int totalCount = featureCursor.getCount();
                int count = 0;
                while (featureCursor.moveToNext()) {
                    try {
                        FeatureRow featureRow = featureCursor.getRow();
                        GeoPackageGeometryData geometryData = featureRow.getGeometry();
                        if (geometryData != null && !geometryData.isEmpty()) {
                            Geometry geometry = geometryData.getGeometry();
                            if (geometry != null) {
                                GoogleMapShape shape = shapeConverter.toShape(geometry);

                                if (shape.getShapeType() == GoogleMapShapeType.LAT_LNG) {
                                    LatLng latLng = (LatLng) shape.getShape();
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    markerOptions.position(latLng);
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
                                    shape = new GoogleMapShape(GeometryType.POINT, GoogleMapShapeType.MARKER_OPTIONS, markerOptions);
                                }

                                GoogleMapShape mapShape = GoogleMapShapeConverter.addShapeToMap(map, shape);

                                if (mapShape.getShapeType() == GoogleMapShapeType.MARKER) {
                                    Marker marker = (Marker) mapShape.getShape();
                                    MarkerFeature markerFeature = new MarkerFeature();
                                    markerFeature.database = geoPackage.getName();
                                    markerFeature.tableName = name;
                                    markerFeature.featureId = featureRow.getId();
                                    markerIds.put(marker.getId(), markerFeature);
                                }

                                tableData.addMapShape(mapShape);

                                if (++count >= maxFeaturesPerTable) {
                                    if (count < totalCount) {
                                        Toast.makeText(context, geoPackage.getName() + "-" + name
                                                + "- added " + count + " of " + totalCount, Toast.LENGTH_LONG).show();
                                    }
                                    break;
                                }
                            }
                        }
                    }catch(Exception e){
                        Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to display feature. GeoPackage: " + geoPackage.getName()
                                + ", Table: " + featureDao.getTableName() + ", Row: " + featureCursor.getPosition(), e);
                    }
                }
            } finally {
                featureCursor.close();
            }
        }
    }

    /**
     * Create Tile Overlay Options for the Tile Provider using the z index
     *
     * @param tileProvider
     * @param zIndex
     * @return
     */
    private TileOverlayOptions createTileOverlayOptions(TileProvider tileProvider, int zIndex) {
        TileOverlayOptions overlayOptions = new TileOverlayOptions();
        overlayOptions.tileProvider(tileProvider);
        overlayOptions.zIndex(zIndex);
        return overlayOptions;
    }

    /**
     * Query and build a map click location message from enabled GeoPackage tables
     *
     * @param latLng click location
     * @return click message
     */
    public String mapClickMessage(LatLng latLng) {
        StringBuilder clickMessage = new StringBuilder();
        if (selectedReport == null) {
            for (GeoPackageMapData data : mapDataList) {
                String message = data.mapClickMessage(latLng, mapView, map);
                if (message != null) {
                    if (clickMessage.length() > 0) {
                        clickMessage.append("\n\n");
                    }
                    clickMessage.append(message);
                }
            }
        }

        return clickMessage.length() > 0 ? clickMessage.toString() : null;
    }

    /**
     * Build a map click location message from the clicked marker
     *
     * @param marker clicked marker
     * @return click message
     */
    public String mapClickMessage(Marker marker) {
        String message = null;
        if (selectedReport == null) {
            MarkerFeature markerFeature = markerIds.get(marker.getId());
            if (markerFeature != null) {
                final GeoPackage geoPackage = manager.open(markerFeature.database);
                try {
                    final FeatureDao featureDao = geoPackage
                            .getFeatureDao(markerFeature.tableName);

                    final FeatureRow featureRow = featureDao.queryForIdRow(markerFeature.featureId);

                    if (featureRow != null) {
                        GeoPackageGeometryData geomData = featureRow.getGeometry();
                        if (geomData != null && !geomData.isEmpty()) {
                            Geometry geometry = geomData.getGeometry();
                            if (geometry != null) {
                                StringBuilder messageBuilder = new StringBuilder();
                                messageBuilder.append(markerFeature.database).append(" - ").append(markerFeature.tableName).append("\n");
                                int geometryColumn = featureRow.getGeometryColumnIndex();
                                for (int i = 0; i < featureRow.columnCount(); i++) {
                                    if (i != geometryColumn) {
                                        Object value = featureRow.getValue(i);
                                        if (value != null) {
                                            messageBuilder.append("\n").append(featureRow.getColumnName(i)).append(": ").append(value);
                                        }
                                    }
                                }

                                if (messageBuilder.length() > 0) {
                                    messageBuilder.append("\n\n");
                                }
                                messageBuilder.append(GeometryPrinter.getGeometryString(geometry));
                                message = messageBuilder.toString();
                            }
                        }
                    }
                } finally {
                    geoPackage.close();
                }
            }
        }
        return message;
    }

    /**
     * Report has been selected on the map
     *
     * @param report selected report
     */
    public void selectedReport(Report report) {

        Report existingReport = selectedReport;

        if (existingReport == null || existingReport != report) {

            if (existingReport != null) {
                deselectedReport();
            }

            if (!report.getCacheFiles().isEmpty()) {

                for (ReportCache reportCache : report.getCacheFiles()) {

                    boolean exists = false;
                    try {
                        exists = manager.exists(reportCache.getName());
                    } catch (Exception e) {
                        Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to check if GeoPackage exists" + reportCache.getName(), e);
                    }

                    if (!exists) {
                        try {
                            manager.importGeoPackageAsExternalLink(reportCache.getPath(), reportCache.getName(), true);
                        } catch (Exception e) {
                            Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to import GeoPackage " + reportCache.getName() + " at path: " + reportCache.getPath(), e);
                        }
                    }
                }

                selectedReport = report;

                updateMap();
            }
        }
    }

    /**
     * Report has been deselected on the map
     */
    public void deselectedReport() {

        boolean change = false;

        if (selectedReport != null) {
            selectedReport = null;

            String like = DICEConstants.DICE_TEMP_CACHE_SUFFIX + "%";
            List<String> geoPackages = null;
            try {
                geoPackages = manager.databasesLike(like);
            } catch (Exception e) {
                Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to find temporary GeoPackages", e);
            }
            if (geoPackages != null) {
                for (String geoPackage : geoPackages) {
                    cache.close(geoPackage);
                    try {
                        manager.delete(geoPackage);
                    } catch (Exception e) {
                        Log.e(GeoPackageMapOverlays.class.getSimpleName(), "Failed to delete GeoPackage: " + geoPackage, e);
                    }
                    change = true;
                }
            }

            if (change) {
                updateMap();
            }
        }
    }

}
