package mil.nga.dice.map.geopackage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.nga.dice.DICEConstants;

/**
 * Retrieve and update selected GeoPackages in the app settings
 */
public class GeoPackageSelected {

    /**
     * Shared preferences
     */
    private final SharedPreferences settings;

    /**
     * Constructor
     *
     * @param context
     */
    public GeoPackageSelected(Context context) {
        settings = PreferenceManager
                .getDefaultSharedPreferences(context);
    }

    /**
     * Get the selected caches
     *
     * @return selected caches
     */
    public Set<String> getSelectedSet() {
        Editor editor = settings.edit();
        return getSelectedSet(editor);
    }

    /**
     * Get the selected caches and their selected tables
     *
     * @return selected cache and table map
     */
    public Map<String, Set<String>> getSelectedMap() {
        Editor editor = settings.edit();
        Set<String> selectedSet = getSelectedSet(editor);
        Map<String, Set<String>> selectedMap = new HashMap<>();
        for (String selected : selectedSet) {
            Set<String> selectedTables = settings.getStringSet(selected, new HashSet<String>());
            selectedMap.put(selected, selectedTables);
        }
        return selectedMap;
    }

    /**
     * Get the selected set of caches
     *
     * @param editor editor
     * @return selected set
     */
    private Set<String> getSelectedSet(Editor editor) {
        Set<String> selected = settings.getStringSet(DICEConstants.DICE_SELECTED_CACHES, new HashSet<String>());
        return new HashSet<>(selected);
    }

    /**
     * Update the selected caches and tables
     *
     * @param selected
     */
    public void updateSelected(Map<String, Set<String>> selected) {
        // TODO
    }

}
