package mil.nga.dice.report;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * Created by stjohnr on 3/18/15.
 */
public class ReportDescriptorUtil {

    private static final String TAG = "ReportDescriptor";

    public static boolean readDescriptorAndUpdateReport(Report report) {
        File metadataFile = new File(report.getPath(), "metadata.json");

        if (!metadataFile.exists()) {
            return false;
        }

        String jsonString = null;
        FileInputStream stream;
        try {
            stream = new FileInputStream(metadataFile);
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "error openining metadata file: " + metadataFile, e);
            return false;
        }

        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            jsonString = Charset.defaultCharset().decode(byteBuffer).toString();
        }
        catch (IOException e) {
            Log.e(TAG, "error reading metadata file: " + metadataFile, e);
        }
        finally {
            try {
                stream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonString);
            if (jsonObject.has("title")) {
                report.setTitle(jsonObject.getString("title"));
            }
            if (jsonObject.has("description")) {
                report.setDescription(jsonObject.getString("description"));
            } else {
                report.setDescription(null);
            }
            if (jsonObject.has("reportID")) {
                report.setId(jsonObject.getString("reportID"));
            }
            if (jsonObject.has("lat")) {
                report.setLat(jsonObject.getDouble("lat"));
            }
            if (jsonObject.has("lon")) {
                report.setLon(jsonObject.getDouble("lon"));
            }
            if (jsonObject.has("thumbnail")) {
                report.setThumbnail(jsonObject.getString("thumbnail"));
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "error parsing json for metadata file: " + metadataFile, e);
            return false;
        }
        return true;
    }

}
