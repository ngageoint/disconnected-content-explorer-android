package mil.nga.dice.report.tests;

import android.os.Environment;
import android.os.Parcel;
import android.test.suitebuilder.annotation.MediumTest;
import junit.framework.TestCase;
import mil.nga.dice.report.Report;

import java.io.File;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


public class ReportTest extends TestCase {

    @MediumTest
    public void testWritesAndReadsToParcel() {
        Report r = new Report();
        r.setDescription("test parcelling");
        r.setEnabled(true);
        r.setError("none");
        r.setId("1234");
        r.setLat(20.0);
        r.setLon(100.0);
        r.setPath(new File(Environment.getExternalStorageDirectory(), "test/reports/test_report"));
        r.setSourceFile(new File(Environment.getExternalStorageDirectory(), "test/report.zip"));
        r.setThumbnail("thumbnail");
        r.setTitle("Test Report");

        Parcel parcel = Parcel.obtain();
        r.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Report fromParcel = Report.CREATOR.createFromParcel(parcel);

        assertThat(fromParcel.getDescription(), equalTo(r.getDescription()));
        assertThat(fromParcel.getError(), equalTo(r.getError()));
        assertThat(fromParcel.getFileExtension(), equalTo(r.getFileExtension()));
        assertThat(fromParcel.getFileName(), equalTo(r.getFileName()));
        assertThat(fromParcel.getId(), equalTo(r.getId()));
        assertThat(fromParcel.getLat(), equalTo(r.getLat()));
        assertThat(fromParcel.getLon(), equalTo(r.getLon()));
        assertThat(fromParcel.getPath(), equalTo(r.getPath()));
        assertThat(fromParcel.getSourceFile(), equalTo(r.getSourceFile()));
        assertThat(fromParcel.getThumbnail(), equalTo(r.getThumbnail()));
        assertThat(fromParcel.getTitle(), equalTo(r.getTitle()));
    }

    @MediumTest
    public void testParcelsNullValues() {
        Report r = new Report();
        r.setDescription(null);
        r.setEnabled(false);
        r.setError(null);
        r.setId(null);
        r.setLat(null);
        r.setLon(null);
        r.setPath(null);
        r.setSourceFile(null);
        r.setThumbnail(null);
        r.setTitle(null);

        Parcel parcel = Parcel.obtain();
        r.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Report fromParcel = Report.CREATOR.createFromParcel(parcel);

        assertNull(fromParcel.getDescription());
        assertFalse(fromParcel.isEnabled());
        assertNull(fromParcel.getError());
        assertNull(fromParcel.getId());
        assertNull(fromParcel.getLat());
        assertNull(fromParcel.getLon());
        assertNull(fromParcel.getPath());
        assertNull(fromParcel.getSourceFile());
        assertNull(fromParcel.getThumbnail());
        assertNull(fromParcel.getTitle());
    }

}
