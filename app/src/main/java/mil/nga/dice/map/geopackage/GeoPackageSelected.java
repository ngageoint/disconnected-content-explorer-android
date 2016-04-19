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
        return settings.getStringSet(DICEConstants.DICE_SELECTED_CACHES, new HashSet<String>());
    }

    /**
     * Get the selected caches and their selected tables
     *
     * @return selected cache and table map
     */
    public Map<String, Set<String>> getSelectedMap() {
        Set<String> selectedSet = settings.getStringSet(DICEConstants.DICE_SELECTED_CACHES, new HashSet<String>());
        Map<String, Set<String>> selectedMap = new HashMap<>();
        for (String selected : selectedSet) {
            Set<String> selectedTables = settings.getStringSet(selected, new HashSet<String>());
            selectedMap.put(selected, selectedTables);
        }
        return selectedMap;
    }

    /**
     * Add selected cache
     *
     * @param cache
     */
    public void addSelected(String cache) {

        Set<String> currentSelectedSet = settings.getStringSet(DICEConstants.DICE_SELECTED_CACHES, new HashSet<String>());
        currentSelectedSet.add(cache);

        Editor editor = settings.edit();
        editor.putStringSet(DICEConstants.DICE_SELECTED_CACHES, currentSelectedSet);
        editor.commit();
    }

    /**
     * Update the selected caches and tables
     *
     * @param selected
     */
    public void updateSelected(Map<String, Set<String>> selected) {

        Set<String> currentSelectedSet = settings.getStringSet(DICEConstants.DICE_SELECTED_CACHES, new HashSet<String>());

        Editor editor = settings.edit();
        editor.putStringSet(DICEConstants.DICE_SELECTED_CACHES, selected.keySet());

        for (Map.Entry<String, Set<String>> selectedEntry : selected.entrySet()) {
            currentSelectedSet.remove(selectedEntry.getKey());
            editor.putStringSet(selectedEntry.getKey(), selectedEntry.getValue());
        }

        for (String currentSelected : currentSelectedSet) {
            editor.remove(currentSelected);
        }

        editor.commit();
    }

    /**
     * Remove selected cache
     *
     * @param cache
     */
    public void removeSelected(String cache) {

        Map<String, Set<String>> selected = getSelectedMap();

        if(selected.containsKey(cache)){
            selected.remove(cache);
            updateSelected(selected);
        }
    }

}
