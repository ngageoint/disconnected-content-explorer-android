package mil.nga.dice.map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.nga.dice.DICEConstants;
import mil.nga.dice.R;
import mil.nga.dice.map.geopackage.GeoPackageMapData;
import mil.nga.dice.map.geopackage.GeoPackageSelected;
import mil.nga.dice.map.geopackage.GeoPackageTableMapData;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.extension.link.FeatureTileTableLinker;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.index.FeatureIndexManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.tiles.user.TileDao;

/**
 * View, enabled, disable, and delete map tile and feature overlays
 */
public class OverlaysActivity extends AppCompatActivity {

    /**
     * Permission request read external storage id
     */
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    /**
     * Overlays adapter as the expandable list adapter
     */
    private OverlaysAdapter overlaysAdapter;

    /**
     * Expandable list view for displaying the GeoPackages and tables
     */
    private ExpandableListView expandableList;

    /**
     * Selected GeoPackages for retrieving and updating selected
     */
    private GeoPackageSelected selectedGeoPackages;

    /**
     * Flag to track when selection changes were made
     */
    private boolean changes = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overlays);

        expandableList = (ExpandableListView) findViewById(R.id.overlays_list);
        expandableList.setEnabled(false);

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                        .setTitle(R.string.overlays_access_title)
                        .setMessage(R.string.overlays_access_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(OverlaysActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                            }
                        })
                        .create()
                        .show();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        overlaysAdapter = new OverlaysAdapter(this);

        expandableList.setAdapter(overlaysAdapter);
        expandableList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                int itemType = ExpandableListView.getPackedPositionType(id);
                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                    int childPosition = ExpandableListView.getPackedPositionChild(id);
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    // Handle child row long clicks here
                    return true;
                } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    int groupPosition = ExpandableListView.getPackedPositionGroup(id);
                    GeoPackageMapData cacheOverlay = (GeoPackageMapData) overlaysAdapter.getGroup(groupPosition);
                    if (!cacheOverlay.isLocked()) {
                        deleteCacheOverlayConfirm(cacheOverlay);
                    }
                    return true;
                }
                return false;
            }
        });

        selectedGeoPackages = new GeoPackageSelected(this);

        update();
    }

    /**
     * Update the overlays table by searching for public GeoPackages and their selected state
     */
    private void update() {

        List<GeoPackageMapData> mapDataList = new ArrayList<>();

        Map<String, Set<String>> selectedCaches = selectedGeoPackages.getSelectedMap();

        GeoPackageManager manager = GeoPackageFactory.getManager(this);

        String like = DICEConstants.DICE_TEMP_CACHE_SUFFIX + "%";
        List<String> geoPackages = manager.databasesNotLike(like);
        for (String name : geoPackages) {
            GeoPackageMapData mapData = new GeoPackageMapData(name);
            Set<String> selectedTables = selectedCaches.get(name);
            if (selectedTables != null) {
                mapData.setEnabled(true);
            }
            mapDataList.add(mapData);

            // TODO Determine if this is a locked GeoPackage
            mapData.setLocked(false);

            GeoPackage geoPackage = manager.open(name);

            // GeoPackage tile tables, build a mapping between table name and the created map overlays
            Map<String, GeoPackageTableMapData> tileMapOverlays = new HashMap<>();
            List<String> tileTables = geoPackage.getTileTables();
            for (String tileTable : tileTables) {
                TileDao tileDao = geoPackage.getTileDao(tileTable);
                GeoPackageTableMapData tableMapData = new GeoPackageTableMapData(tileTable, false);
                if (tableMapData.isEnabled() && (selectedTables.size() == 0 || selectedTables.contains(tileTable))) {
                    tableMapData.setEnabled(true);
                }
                tableMapData.setCount(tileDao.count());
                tableMapData.setMinZoom(tileDao.getMinZoom());
                tableMapData.setMaxZoom(tileDao.getMaxZoom());
                tileMapOverlays.put(tileTable, tableMapData);
            }

            // Get a linker to find tile tables linked to features
            FeatureTileTableLinker linker = new FeatureTileTableLinker(geoPackage);
            Map<String, GeoPackageTableMapData> linkedTileMapOverlays = new HashMap<>();

            List<String> featureTables = geoPackage.getFeatureTables();
            for (String featureTable : featureTables) {
                FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
                FeatureIndexManager indexer = new FeatureIndexManager(this, geoPackage, featureDao);
                boolean indexed = indexer.isIndexed();
                long minZoom = 0;
                if (indexed) {
                    minZoom = featureDao.getZoomLevel() + DICEConstants.DICE_FEATURE_TILES_MIN_ZOOM_OFFSET;
                    minZoom = Math.max(minZoom, 0);
                    minZoom = Math.min(minZoom, DICEConstants.DICE_FEATURES_MAX_ZOOM);
                }
                GeoPackageTableMapData tableMapData = new GeoPackageTableMapData(featureTable, true);
                if (mapData.isEnabled() && (selectedTables.size() == 0 || selectedTables.contains(featureTable))) {
                    tableMapData.setEnabled(true);
                }
                tableMapData.setCount(featureDao.count());
                tableMapData.setMinZoom(minZoom);
                tableMapData.setMaxZoom(DICEConstants.DICE_FEATURES_MAX_ZOOM);

                // If indexed, check for linked tile tables
                if (indexed) {
                    List<String> linkedTileTables = linker.getTileTablesForFeatureTable(featureTable);
                    for (String linkedTileTable : linkedTileTables) {
                        // Get the tile table cache overlay
                        GeoPackageTableMapData tileTableMapData = tileMapOverlays.get(linkedTileTable);
                        if (tileTableMapData != null) {
                            // Remove from tile cache overlays so the tile table is not added as stand alone, and add to the linked overlays
                            tileMapOverlays.remove(linkedTileTable);
                            linkedTileMapOverlays.put(linkedTileTable, tileTableMapData);
                        } else {
                            // Another feature table may already be linked to this table, so check the linked overlays
                            tileTableMapData = linkedTileMapOverlays.get(linkedTileTable);
                        }

                        // Add the linked tile table to the feature table
                        if (tileTableMapData != null) {
                            tableMapData.addLinked(tileTableMapData);
                        }
                    }
                }

                mapData.addTable(tableMapData);
            }

            // Add stand alone tile tables that were not linked to feature tables
            for (GeoPackageTableMapData tileCacheOverlay : tileMapOverlays.values()) {
                mapData.addTable(tileCacheOverlay);
            }

            geoPackage.close();
        }

        overlaysAdapter.setOverlays(mapDataList);
        overlaysAdapter.notifyDataSetChanged();

        expandableList.setEnabled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        if (changes || overlaysAdapter.isChanges()) {
            selectedGeoPackages.updateSelected(getSelected());
        }
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO
                }

                break;
            }
        }
    }

    /**
     * Cache Overlay Expandable list adapter
     */
    public static class OverlaysAdapter extends BaseExpandableListAdapter {

        /**
         * Context
         */
        private OverlaysActivity activity;

        /**
         * GeoPackage Map data
         */
        private List<GeoPackageMapData> overlays = new ArrayList<>();

        /**
         * Flag to track when selection changes were made
         */
        private boolean changes = false;

        /**
         * Constructor
         *
         * @param activity
         */
        public OverlaysAdapter(OverlaysActivity activity) {
            this.activity = activity;
        }

        /**
         * Set the overlays
         *
         * @param overlays
         */
        public void setOverlays(List<GeoPackageMapData> overlays) {
            this.overlays = overlays;
        }

        /**
         * Is there changes
         *
         * @return true if changes
         */
        public boolean isChanges() {
            return changes;
        }

        /**
         * Get the overlays
         *
         * @return
         */
        public List<GeoPackageMapData> getOverlays() {
            return overlays;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getGroupCount() {
            return overlays.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getChildrenCount(int i) {
            return overlays.get(i).getTables().size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getGroup(int i) {
            return overlays.get(i);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getChild(int i, int j) {
            return overlays.get(i).getTables().get(j);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getGroupId(int i) {
            return i;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getChildId(int i, int j) {
            return j;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasStableIds() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getGroupView(int i, boolean isExpanded, View view,
                                 ViewGroup viewGroup) {
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(activity);
                view = inflater.inflate(R.layout.overlays_group, viewGroup, false);
            }

            ImageView imageView = (ImageView) view.findViewById(R.id.overlays_group_image);
            TextView cacheName = (TextView) view.findViewById(R.id.overlays_group_name);
            ImageView lockedImageView = (ImageView) view.findViewById(R.id.overlays_group_locked_image);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.overlays_group_checkbox);

            final GeoPackageMapData overlay = overlays.get(i);

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();

                    overlay.setEnabled(checked);
                    changes = true;

                    boolean modified = false;
                    for (GeoPackageTableMapData childCache : overlay.getTables()) {
                        if (childCache.isEnabled() != checked) {
                            childCache.setEnabled(checked);
                            modified = true;
                        }
                    }

                    if (modified) {
                        notifyDataSetChanged();
                    }
                }
            });

            Integer imageResource = overlay.getIconImageResourceId();
            if (imageResource != null) {
                imageView.setImageResource(imageResource);
            } else {
                imageView.setImageResource(-1);
            }
            cacheName.setText(overlay.getName());
            if (overlay.isLocked()) {
                lockedImageView.setVisibility(View.VISIBLE);
            } else {
                lockedImageView.setVisibility(View.INVISIBLE);
            }
            checkBox.setChecked(overlay.isEnabled());

            return view;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(activity);
                convertView = inflater.inflate(R.layout.overlays_child, parent, false);
            }

            final GeoPackageMapData overlay = overlays.get(groupPosition);
            final GeoPackageTableMapData childCache = overlay.getTables().get(childPosition);

            ImageView imageView = (ImageView) convertView.findViewById(R.id.overlays_child_image);
            TextView tableName = (TextView) convertView.findViewById(R.id.overlays_child_name);
            TextView info = (TextView) convertView.findViewById(R.id.overlays_child_info);
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.overlays_child_checkbox);

            convertView.findViewById(R.id.divider).setVisibility(isLastChild ? View.VISIBLE : View.INVISIBLE);

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox) v).isChecked();

                    childCache.setEnabled(checked);
                    changes = true;

                    boolean modified = false;
                    if (checked) {
                        if (!overlay.isEnabled()) {
                            overlay.setEnabled(true);
                            modified = true;
                        }
                    } else if (overlay.isEnabled()) {
                        modified = true;
                        for (GeoPackageTableMapData childCache : overlay.getTables()) {
                            if (childCache.isEnabled()) {
                                modified = false;
                                break;
                            }
                        }
                        if (modified) {
                            overlay.setEnabled(false);
                        }
                    }

                    if (modified) {
                        notifyDataSetChanged();
                    }
                }
            });

            tableName.setText(childCache.getName());
            info.setText(childCache.getInfo());
            checkBox.setChecked(childCache.isEnabled());

            Integer imageResource = childCache.getIconImageResourceId();
            if (imageResource != null) {
                imageView.setImageResource(imageResource);
            } else {
                imageView.setImageResource(-1);
            }

            return convertView;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isChildSelectable(int i, int j) {
            return true;
        }

    }

    /**
     * Delete the cache overlay
     *
     * @param cacheOverlay
     */
    private void deleteCacheOverlayConfirm(final GeoPackageMapData cacheOverlay) {
        AlertDialog deleteDialog = new AlertDialog.Builder(this)
                .setTitle("Delete Cache")
                .setMessage("Delete " + cacheOverlay.getName() + " Cache?")
                .setPositiveButton("Delete",

                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                deleteCacheOverlay(cacheOverlay);
                            }
                        })

                .setNegativeButton(getString(R.string.button_cancel_label),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        }).create();
        deleteDialog.show();
    }

    /**
     * Delete the cache overlay
     *
     * @param cacheOverlay
     */
    private void deleteCacheOverlay(GeoPackageMapData cacheOverlay) {

        expandableList.setEnabled(false);

        // Get the GeoPackage file
        GeoPackageManager manager = GeoPackageFactory.getManager(this);

        // Delete the cache from the GeoPackage manager
        manager.delete(cacheOverlay.getName());

        changes = true;

        update();
    }

    /**
     * Get the selected GeoPackages and tables
     *
     * @return selected map
     */
    private Map<String, Set<String>> getSelected() {

        Map<String, Set<String>> selected = new HashMap<>();

        for (GeoPackageMapData mapData : overlaysAdapter.getOverlays()) {

            if (mapData.isEnabled()) {
                Set<String> selectedTables = new HashSet<>();
                selected.put(mapData.getName(), selectedTables);

                for (GeoPackageTableMapData tableMapData : mapData.getTables()) {

                    if (tableMapData.isEnabled()) {
                        selectedTables.add(tableMapData.getName());
                    }

                    for (GeoPackageTableMapData linkedTable : tableMapData.getLinked()) {
                        if (linkedTable.isEnabled()) {
                            selectedTables.add(linkedTable.getName());
                        }
                    }
                }
            }

        }

        return selected;
    }

}