package mil.nga.dice.report;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.maps.model.LatLng;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.nga.dice.DICEConstants;
import mil.nga.dice.io.DICEFileUtils;
import mil.nga.dice.map.geopackage.GeoPackageMapData;
import mil.nga.dice.map.geopackage.GeoPackageTableMapData;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageCache;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.tiles.features.FeatureTiles;
import mil.nga.geopackage.tiles.features.MapFeatureTiles;
import mil.nga.geopackage.tiles.overlay.BoundedOverlay;
import mil.nga.geopackage.tiles.overlay.FeatureOverlay;
import mil.nga.geopackage.tiles.overlay.FeatureOverlayQuery;
import mil.nga.geopackage.tiles.overlay.GeoPackageOverlayFactory;
import mil.nga.geopackage.tiles.retriever.GeoPackageTile;
import mil.nga.geopackage.tiles.retriever.GeoPackageTileRetriever;
import mil.nga.geopackage.tiles.user.TileDao;
import mil.nga.geopackage.validate.GeoPackageValidate;

/**
 * GeoPackage Web View for intercepting and responding to GeoPackage tile URL requests
 */
public class GeoPackageWebViewClient extends WebViewClient {

    /**
     * Table URL parameter
     */
    private static final String TABLE_PARAM = "table";

    /**
     * Zoom URL parameter
     */
    private static final String ZOOM_PARAM = "z";

    /**
     * X URL Parameter
     */
    private static final String X_PARAM = "x";

    /**
     * Y URL Parameter
     */
    private static final String Y_PARAM = "y";

    /**
     * Application context
     */
    private final Context context;

    /**
     * GeoPackage manager
     */
    private final GeoPackageManager manager;

    /**
     * GeoPackage cache
     */
    private final GeoPackageCache cache;

    /**
     * Report Id
     */
    private final String reportId;

    /**
     * Map Data map
     */
    private final Map<String, GeoPackageMapData> mapData = new HashMap<>();

    /**
     * Constructor
     *
     * @param context  app context
     * @param reportId report id
     */
    public GeoPackageWebViewClient(Context context, String reportId) {
        this.context = context;
        manager = GeoPackageFactory.getManager(context);
        cache = new GeoPackageCache(manager);
        this.reportId = reportId;
    }

    /**
     * Close the GeoPackage connections
     */
    public void close() {
        cache.closeAll();
        String like = DICEConstants.DICE_TEMP_CACHE_SUFFIX + "%";
        List<String> geoPackages = null;
        try {
            geoPackages = manager.databasesLike(like);
        } catch (Exception e) {
            Log.e(GeoPackageWebViewClient.class.getSimpleName(), "Failed to find temporary GeoPackages", e);
        }
        if (geoPackages != null) {
            for (String geoPackage : geoPackages) {
                manager.delete(geoPackage);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Handle post lollipop
     */
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse response = null;
        if (request != null) {
            Uri url = request.getUrl();
            if (url != null) {
                String path = url.getPath();
                File file = new File(path);
                if (GeoPackageValidate.hasGeoPackageExtension(file)) {

                    List<String> tables = url.getQueryParameters(TABLE_PARAM);
                    String zoom = url.getQueryParameter(ZOOM_PARAM);
                    String x = url.getQueryParameter(X_PARAM);
                    String y = url.getQueryParameter(Y_PARAM);

                    response = handleUrl(file, tables, zoom, x, y);
                }
            }
        }

        return response;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Handle pre lollipop // TODO remove when minimum Android version is at or above 21
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return null; // TODO
    }

    private WebResourceResponse handleUrl(File file, List<String> tables, String zoom, String x, String y) {

        WebResourceResponse response = null;

        if (tables != null && !tables.isEmpty() && zoom != null && x != null && y != null) {

            int zoomValue = Integer.parseInt(zoom);
            int xValue = Integer.parseInt(x);
            int yValue = Integer.parseInt(y);

            String nameWithExtension = file.getName();
            String name = DICEFileUtils.removeExtension(nameWithExtension);

            String localPath = ReportUtils.localReportPath(file);
            String sharedPrefix = reportId + File.separator + DICEConstants.DICE_REPORT_SHARED_DIRECTORY;
            boolean shared = localPath.startsWith(sharedPrefix);

            name = reportId(name, reportId, shared);

            GeoPackage geoPackage = null;

            if (name != null) {

                if (manager.exists(name)) {
                    try {
                        geoPackage = cache.getOrOpen(name);
                    } catch (Exception e) {
                        cache.close(name);
                        manager.delete(name);
                        geoPackage = null;
                    }
                }

                if (geoPackage == null) {

                    File importFile = file;

                    // If a shared file, check if the file exists in this report or another
                    if (shared) {

                        // If the file is not in this report, search other reports
                        if (!importFile.exists()) {

                            // TODO
                            //NSString * sharedSearchPath = [localPath substringFromIndex:[currentId length]];

                            File[] reportDirectories = ReportUtils.getReportDirectories(context);
                            for (File reportDirectory : reportDirectories) {

                                File sharedLocation = new File("."); // TODO
                                //NSString * sharedLocation = [NSString stringWithFormat:@"%@%@", reportDirectory, sharedSearchPath];

                                if (sharedLocation.exists()) {
                                    importFile = sharedLocation;
                                    break;
                                }
                            }

                        }
                    }

                    if (importFile.exists()) {
                        manager.importGeoPackageAsExternalLink(importFile, name);
                        try {
                            geoPackage = cache.getOrOpen(name);
                        } catch (Exception e) {
                            // TODO
                            geoPackage = null;
                        }
                    }
                }
            }

            byte[] tileData = null;

            if (geoPackage != null) {
                for (String table : tables) {

                    // Get or create the GeoPackage data
                    GeoPackageMapData geoPackageData = mapData.get(name);
                    if (geoPackageData == null) {
                        geoPackageData = new GeoPackageMapData(name);
                        mapData.put(name, geoPackageData);
                    }
                    // Get or create the table data
                    GeoPackageTableMapData tableData = geoPackageData.getTable(table);
                    if (tableData == null) {
                        tableData = new GeoPackageTableMapData(table, geoPackage.isFeatureTable(table));
                        geoPackageData.addTable(tableData);
                    } else {
                        // Feature Overlay Queries have already been added for this table
                        tableData = null;
                    }

                    if (geoPackage.isTileTable(table)) {

                        TileDao tileDao = geoPackage.getTileDao(table);

                        GeoPackageTileRetriever retriever = new GeoPackageTileRetriever(tileDao);
                        if (retriever.hasTile(xValue, yValue, zoomValue)) {
                            GeoPackageTile tile = retriever.getTile(xValue, yValue, zoomValue);
                            if (tile != null) {
                                tileData = tile.getData();
                            }
                        }

                        // If the first time handling this table
                        if (tableData != null) {
                            // Check for linked feature tables
                            BoundedOverlay geoPackageTileOverlay = GeoPackageOverlayFactory.getBoundedOverlay(tileDao);
                            FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
                            List<FeatureDao> featureDaos = linker.getFeatureDaosForTileTable(tileDao.getTableName());
                            for (FeatureDao featureDao : featureDaos) {

                                // Create the feature tiles
                                FeatureTiles featureTiles = new MapFeatureTiles(context, featureDao);

                                // Create an index manager
                                FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
                                featureTiles.setIndexManager(indexer);

                                // Add the feature overlay query
                                FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(context, geoPackageTileOverlay, featureTiles);
                                tableData.addFeatureOverlayQuery(featureOverlayQuery);
                            }
                        }

                    } else if (geoPackage.isFeatureTable(table)) {

                        FeatureDao featureDao = geoPackage.getFeatureDao(table);
                        FeatureTiles featureTiles = new MapFeatureTiles(context, featureDao);
                        FeatureIndexManager indexer = new FeatureIndexManager(context, geoPackage, featureDao);
                        featureTiles.setIndexManager(indexer);
                        if (featureTiles.isIndexQuery() && featureTiles.queryIndexedFeaturesCount(xValue, yValue, zoomValue) > 0) {
                            tileData = featureTiles.drawTileBytes(xValue, yValue, zoomValue);
                        }

                        if (tableData != null && featureTiles.isIndexQuery()) {
                            featureTiles.setIndexManager(indexer);

                            FeatureOverlay featureOverlay = new FeatureOverlay(featureTiles);
                            featureOverlay.setMinZoom(featureDao.getZoomLevel());

                            FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
                            List<TileDao> tileDaos = linker.getTileDaosForFeatureTable(featureDao.getTableName());
                            featureOverlay.ignoreTileDaos(tileDaos);

                            FeatureOverlayQuery featureOverlayQuery = new FeatureOverlayQuery(context, featureOverlay);
                            tableData.addFeatureOverlayQuery(featureOverlayQuery);
                        }

                    }

                    if (tileData != null) {
                        break;
                    }

                }
            }

            if (tileData != null) {
                InputStream is = new ByteArrayInputStream(tileData);
                response = new WebResourceResponse("text/html", "UTF-8", is);
            }
        }

        return response;
    }

    /**
     * Get the report id prefix
     *
     * @param report report name
     * @return report id prefix
     */
    public static String reportIdPrefix(String report) {
        String reportIdPrefix = report;
        if (reportIdPrefix != null) {
            reportIdPrefix = DICEConstants.DICE_TEMP_CACHE_SUFFIX + reportIdPrefix + "-";
        }
        return reportIdPrefix;
    }

    /**
     * Get the report id with prefix if needed
     *
     * @param name   cache name
     * @param report report name
     * @param share  true if a shared cache
     * @return report id
     */
    public static String reportId(String name, String report, boolean share) {
        String reportId = name;
        if (!share) {
            String reportIdPrefix = reportIdPrefix(report);
            if (reportIdPrefix != null) {
                reportId = reportIdPrefix + reportId;
            } else {
                reportId = null;
            }
        }
        return reportId;
    }

    /**
     * Get a message from a map click
     *
     * @param latLng    click location
     * @param zoom      zoom level
     * @param mapBounds map bounding box
     * @return click message
     */
    public String mapClickMessage(LatLng latLng, double zoom, BoundingBox mapBounds) {
        StringBuilder clickMessage = new StringBuilder();
        for (GeoPackageMapData geoPackageData : mapData.values()) {
            String message = geoPackageData.mapClickMessage(latLng, zoom, mapBounds);
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
     * Get the table data from a map click
     *
     * @param latLng            click location
     * @param zoom              zoom level
     * @param mapBounds         map bounding box
     * @param includePoints     true to include point information
     * @param includeGeometries true to include all geometry information
     * @return map of table data
     */
    public Map<String, Object> mapClickTableData(LatLng latLng, double zoom, BoundingBox mapBounds, boolean includePoints, boolean includeGeometries) {
        Map<String, Object> clickData = new HashMap<>();
        for (GeoPackageMapData geoPackageData : mapData.values()) {
            Map<String, Object> geoPackageClickData = geoPackageData.mapClickTableData(latLng, zoom, mapBounds, includePoints, includeGeometries);
            if (geoPackageClickData != null) {
                clickData.put(geoPackageData.getName(), geoPackageClickData);
            }
        }
        return clickData.size() > 0 ? clickData : null;
    }

}
