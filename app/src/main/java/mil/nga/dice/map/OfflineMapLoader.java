package mil.nga.dice.map;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import mil.nga.dice.jackson.deserializer.FeatureDeserializer;
import android.content.Context;
import android.os.AsyncTask;

import com.vividsolutions.jts.geom.Geometry;

public class OfflineMapLoader {

	private Collection<Geometry> offlineFeatures = null;
	private Collection<OnOfflineFeaturesListener> offlineFeaturesListeners = new ArrayList<OnOfflineFeaturesListener>();
	private Context mContext;
	
	public interface OnOfflineFeaturesListener {
		public void onOfflineFeaturesLoaded(Collection<Geometry> offlineFeatures);
	}
	
	
	public OfflineMapLoader(Context context) {
		this.mContext = context;
		loadOfflineMap();
	}
	
	
	public void registerOfflineMapListener(OnOfflineFeaturesListener listener) {
	    offlineFeaturesListeners.add(listener);
		if (offlineFeatures != null)
			listener.onOfflineFeaturesLoaded(offlineFeatures);
	}

	
	public void unregisterOfflineMapListener(OnOfflineFeaturesListener listener) {
	    offlineFeaturesListeners.remove(listener);
	}
	
	
	public void loadOfflineMap() {
		OfflineMapTask task = new OfflineMapTask();
		task.execute();
	}

	
	private void setOfflineMap(Collection<Geometry> offlineFeatures) {
		this.offlineFeatures = offlineFeatures;

		for (OnOfflineFeaturesListener listener : offlineFeaturesListeners) {
			listener.onOfflineFeaturesLoaded(offlineFeatures);
		}
	}

	
	private class OfflineMapTask extends AsyncTask<Void, Void, Collection<Geometry>> {
	    private static final String OFFLINE_MAP_FILENAME = "ne_110m_land.geojson";
	    
		@Override
		protected Collection<Geometry> doInBackground(Void... params) {
		    Collection<Geometry> geometries = new ArrayList<Geometry>();
            InputStream is = null;
            try {
                is = mContext.getAssets().open(OFFLINE_MAP_FILENAME);
                geometries = new FeatureDeserializer().parseFeatures(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
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
		protected void onPostExecute(Collection<Geometry> features) {
		    setOfflineMap(features);
		}
	}
}
