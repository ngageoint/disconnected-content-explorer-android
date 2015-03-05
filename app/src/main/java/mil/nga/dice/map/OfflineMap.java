package mil.nga.dice.map;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.TimeUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import mil.nga.dice.jackson.deserializer.FeatureDeserializer;

public class OfflineMap {
    private static final String TAG = "OfflineMap";

    private static final int FILL_COLOR = 0xFFDDDDDD;

    private static final List<OfflineMap> waitingForPolygons = new ArrayList<>();

    private static boolean initialized = false;
    private static Collection<PolygonOptions> polygonOptions;

    public static synchronized void initialize(Context context) {
        if (initialized) {
            throw new Error("attempted to initialize " + OfflineMap.class.getName() + " more than once");
        }
        initialized = true;
        new LoadOfflineMapTask(context).execute();
    }


    private final GoogleMap map;
    private final TileOverlay backgroundTileOverlay;

    private Collection<Polygon> polygons;

    public OfflineMap(GoogleMap map) {
        this.map = map;
        backgroundTileOverlay = map.addTileOverlay(new TileOverlayOptions()
                .tileProvider(BackgroundTileProvider.getInstance())
                .visible(false)
                .zIndex(1));
    }

    public void setVisible(boolean visible) {
        Log.d(TAG, "set visible " + visible);
        backgroundTileOverlay.setVisible(visible);

        if (polygonOptions == null) {
            Log.d(TAG, "waiting for polygons");
            waitingForPolygons.add(this);
            return;
        }
        else if (polygons == null) {
            polygonsReady();
        }
        else {
            for (Polygon polygon : polygons) {
                polygon.setVisible(visible);
            }
        }
    }

    public boolean isVisible() {
        return backgroundTileOverlay.isVisible();
    }

    public void clear() {
        Log.d(TAG, "clearing offline map");
        backgroundTileOverlay.clearTileCache();
        backgroundTileOverlay.remove();
        for (Polygon polygon : polygons) {
            polygon.remove();
        }
        polygons.clear();
    }

    private void polygonsReady() {
        Log.d(TAG, "adding " + polygonOptions.size() + " polygons to map");
        polygons = new ArrayList<Polygon>(polygonOptions.size());
        for (PolygonOptions polygon : polygonOptions) {
            polygon.visible(isVisible());
            polygons.add(map.addPolygon(polygon));
        }
        Log.d(TAG, "polygons added to map");
    }

    private static class LoadOfflineMapTask extends AsyncTask<Void, Void, List<Geometry>> {
        private static final String OFFLINE_MAP_FILENAME = "ne_110m_land.geojson";

        private final Context context;

        public LoadOfflineMapTask(Context context) {
            this.context = context;
        }

        @Override
        protected List<Geometry> doInBackground(Void... params) {
            // TODO: parse geojson directly into PolygonOptions
            List<Geometry> geometries = new ArrayList<>();
            InputStream is = null;
            try {
                is = context.getAssets().open(OFFLINE_MAP_FILENAME);
                geometries = new FeatureDeserializer().parseFeatures(is);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }

            return geometries;
        }

        @Override
        protected void onPostExecute(List<Geometry> features) {
            new TransformOfflinePolygonsTask(features).execute();
        }
    }

    private static class TransformOfflinePolygonsTask extends AsyncTask<Void, Void, List<PolygonOptions>> {

        private final List<Geometry> features;

        private long startTime;

        public TransformOfflinePolygonsTask(List<Geometry> features) {
            this.features = features;
        }

        private PolygonOptions transformPolygon(com.vividsolutions.jts.geom.Polygon polygon) {
            PolygonOptions options = new PolygonOptions()
                    .zIndex(2)
                    .visible(false)
                    .fillColor(FILL_COLOR)
                    .strokeWidth(0);

            for (Coordinate coordinate : polygon.getExteriorRing().getCoordinates()) {
                options.add(new LatLng(coordinate.y, coordinate.x));
            }

            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                Coordinate[] coordinates = polygon.getInteriorRingN(0).getCoordinates();
                Collection<LatLng> hole = new ArrayList<>(coordinates.length);
                for (Coordinate coordinate : coordinates) {
                    hole.add(new LatLng(coordinate.y, coordinate.x));
                }
                options.addHole(hole);
            }

            return options;
        }

        @Override
        protected List<PolygonOptions> doInBackground(Void... voids) {
            startTime = System.currentTimeMillis();
            Log.d(TAG, "transforming geometries to polygons");

            List<PolygonOptions> polygons = new ArrayList<>(features.size());

            for (Geometry feature : features) {
                // For now all offline map features are polygons
                if ("Polygon".equals(feature.getGeometryType())) {
                    polygons.add(transformPolygon((com.vividsolutions.jts.geom.Polygon) feature));
                }
                else if ("MultiPolygon".equals(feature.getGeometryType())) {
                    MultiPolygon mp = (MultiPolygon) feature;
                    for (int i = 0; i < mp.getNumGeometries(); i++) {
                        com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) mp.getGeometryN(i);
                        polygons.add(transformPolygon(polygon));
                    }
                }
            }
            
            return polygons;
        }

        @Override
        protected void onPostExecute(List<PolygonOptions> polygons) {
            long stopTime = System.currentTimeMillis();

            Log.d(TAG, "finished transforming geometries to polygons after " + (stopTime - startTime) + "ms");

            polygonOptions = polygons;
            Iterator<OfflineMap> waitingMaps = waitingForPolygons.iterator();
            while (waitingMaps.hasNext()) {
                Log.d(TAG, "notify polygons ready");
                waitingMaps.next().polygonsReady();
                waitingMaps.remove();
            }
        }
    }
}
