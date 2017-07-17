package mil.nga.dice.io;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.File;

/**
 * DICE File Utilities
 */
public class DICEFileUtils {

    /**
     * Get display name from the uri
     *
     * @param context
     * @param uri
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getDisplayName(Context context, Uri uri) {

        String name = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver resolver = context.getContentResolver();
            Cursor nameCursor = resolver.query(uri, null, null, null, null);
            try {
                if (nameCursor.getCount() > 0) {
                    int displayNameIndex = nameCursor
                            .getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                    if (displayNameIndex >= 0 && nameCursor.moveToFirst()) {
                        name = nameCursor.getString(displayNameIndex);
                    }
                }
            } finally {
                nameCursor.close();
            }
        }

        if (name == null) {
            name = uri.getPath();
            int index = name.lastIndexOf('/');
            if (index != -1) {
                name = name.substring(index + 1);
            }
        }

        return name;
    }

    /**
     * Get the display name from the URI and path
     *
     * @param context
     * @param uri
     * @param path
     * @return
     */
    public static String getDisplayName(Context context, Uri uri, String path) {

        // Try to get the GeoPackage name
        String name = null;
        if (path != null) {
            name = new File(path).getName();
        } else {
            name = getDisplayName(context, uri);
        }

        return name;
    }

    /**
     * Get the display name from the URI and path
     *
     * @param context
     * @param uri
     * @return
     */
    public static String getDisplayNameWithoutExtension(Context context, Uri uri) {
        return getDisplayNameWithoutExtension(context, uri, null);
    }

    /**
     * Get the display name from the URI and path
     *
     * @param context
     * @param uri
     * @param path
     * @return
     */
    public static String getDisplayNameWithoutExtension(Context context, Uri uri, String path) {

        // Try to get the GeoPackage name
        String name = getDisplayName(context, uri, path);

        // Remove the extension
        name = removeExtension(name);

        return name;
    }

    /**
     * Remove the extension from the file name or path
     * @param name
     * @return
     */
    public static String removeExtension(String name){
        // Remove the extension
        if (name != null) {
            int extensionIndex = name.lastIndexOf(".");
            if (extensionIndex > -1) {
                name = name.substring(0, extensionIndex);
            }
        }

        return name;
    }

    /**
     * Attempt to detect temporary file paths so that the files can be copied as needed
     * @param path
     * @return true if a temporary file path
     */
    public static boolean isTemporaryPath(String path){
        boolean temporary = isGoogleDocsInternalPath(path);
        return temporary;
    }

    /**
     * Determine if the file path is an internal Google docs / drive path
     * @param path file path
     * @return true if an internal path
     */
    public static boolean isGoogleDocsInternalPath(String path){
        return path.contains("com.google.android.apps.docs/files/fileinternal");
    }

}
