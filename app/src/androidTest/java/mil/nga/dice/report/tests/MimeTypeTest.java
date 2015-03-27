package mil.nga.dice.report.tests;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import junit.framework.TestCase;

/**
 * Created by stjohnr on 3/27/15.
 */
public class MimeTypeTest extends TestCase {

    public void testGetFileExtension() {
        String url = "file:///spaces in url/more spaces.html?ner1=1&ner2=2#fragment";
        String ext = MimeTypeMap.getFileExtensionFromUrl(Uri.encode(Uri.parse(url).getEncodedPath()));

        assertEquals("html", ext);
    }

}
