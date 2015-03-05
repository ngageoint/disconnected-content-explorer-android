package mil.nga.dice.map;

import android.content.Context;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.IOException;
import java.io.InputStream;


public class BackgroundTileProvider implements TileProvider {

    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;

    private static Tile tile;
    private static BackgroundTileProvider instance;

    public static synchronized void initialize(Context context) {
        if (instance != null) {
            throw new Error("attempted to initialize " + BackgroundTileProvider.class.getName() + " more than once");
        }

        InputStream in = null;
        try {
            in = context.getAssets().open("background_tile.png");
            byte[] data = new byte[in.available()];
            in.read(data);
            tile = new Tile(TILE_WIDTH, TILE_HEIGHT, data);
        }
        catch (IOException e) {
            throw new Error("failed to load offline tile asset", e);
        }
        finally {
            if (in != null)
                try { in.close(); }
                catch (Exception ignored) {}
        }

        instance = new BackgroundTileProvider();
    }

    public static BackgroundTileProvider getInstance() {
        if (instance == null) {
            throw new Error(BackgroundTileProvider.class.getName() + " not initialized");
        }
        return instance;
    }

    private BackgroundTileProvider() {
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {        
        return tile;
    }

}
