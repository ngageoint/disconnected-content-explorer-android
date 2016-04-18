package mil.nga.dice.report;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Report implements Parcelable {
	private Uri sourceFile;
    private String sourceFileName;
    private long sourceFileSize;
	private File path;
	private String title;
	private String description;
	private String thumbnail;
	private String id;
	private String error;
	private Double lat;
	private Double lon;
	private boolean enabled = false;
	private List<ReportCache> cacheFiles = new ArrayList<>();


	public Report() {}


	public String toString() {
        return "Report: " + title + " (id:" + id + ")";
	}

	/**
	 * Return the {@link android.net.Uri Uri} path to the original file from which this report was imported, e.g., a downloaded zip file.
     * This will most likely be either a file:// URI or content:// URI.
	 * @return {@link String} or null
	 */
	public Uri getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(Uri x) {
		sourceFile = x;
	}

    /**
     * Return the human-relevant file name of the {@link #getSourceFile() source file}.  If the source file URI is a file:// URI, this will be the last
     * component of that URI path.  If the source file is a content:// URI, this value would have been retrieved through the
     * {@link android.content.ContentResolver} API, or set to some other meaningful name.
     * @return {@link String} or null
     */
    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String x) {
        sourceFileName = x;
    }

    public long getSourceFileSize() {
        return sourceFileSize;
    }

    public void setSourceFileSize(long x) {
        sourceFileSize = x;
    }

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getFileExtension() {
		String fileName = getSourceFileName();
		int lastDot = fileName.lastIndexOf(".");
		if (lastDot > -1 && lastDot < fileName.length() - 1) {
			return fileName.substring(lastDot + 1);
		}
		return null;
	}

	/**
	 * Return the absolute path to the report content.  For a typical HTML-based report from a zip file, this should the path to the root directory of the
     * Web content.  For single file formats like PDF, this will be the path directly to that file.
	 * @return
	 */
	public File getPath() {
		return path;
	}
	
	public void setPath(File x) {
		path = x;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getError() {
		return error;
	}
	
	public void setError(String error) {
		this.error = error;
	}
	
	public Double getLat () {
		return lat;
	}
	
	public void setLat (Double lat) {
		this.lat = lat;
	}
	
	public Double getLon () {
		return lon;
	}
	
	public void setLon (Double lon) {
		this.lon = lon;
	}
	
	public Boolean isEnabled () {
		return enabled;
	}
	
	public void setEnabled (boolean enabled) {
		this.enabled = enabled;
	}

	public List<ReportCache> getCacheFiles() {
		return cacheFiles;
	}

	public void addReportCache(ReportCache reportCache){
		cacheFiles.add(reportCache);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		if (description != null)
			parcel.writeString(description);
		else
			parcel.writeValue(null);

		parcel.writeByte((byte) (enabled ? 1 : 0));

		if (error != null)
			parcel.writeString(error);
		else
			parcel.writeValue(null);

		if (id != null)
			parcel.writeString(id);
		else
			parcel.writeValue(null);

		parcel.writeValue(lat);

		parcel.writeValue(lon);

        if (path != null)
            parcel.writeString(path.getAbsolutePath());
        else
            parcel.writeValue(null);

        if (sourceFile != null)
            parcel.writeValue(sourceFile);
        else
            parcel.writeValue(null);

        if (sourceFileName != null)
            parcel.writeString(sourceFileName);
        else
            parcel.writeValue(null);

        parcel.writeLong(sourceFileSize);

        if (thumbnail != null)
			parcel.writeString(thumbnail);
		else
			parcel.writeValue(null);

		if (title != null)
			parcel.writeString(title);
		else
			parcel.writeValue(null);

	}

	public static final Parcelable.Creator<Report> CREATOR = new Creator<Report> () {
		@Override
		public Report createFromParcel(Parcel source) {
			Report report = new Report();

			report.description = source.readString();
			report.enabled = source.readByte() != 0;
			report.error = source.readString();
			report.id = source.readString();
			report.lat = (Double) source.readValue(null);
			report.lon = (Double) source.readValue(null);
            Object value = source.readString();
            if (value != null) {
                report.path = new File((String) value);
            }
            report.sourceFile = (Uri) source.readValue(null);
            report.sourceFileName = source.readString();
            report.sourceFileSize = source.readLong();
			report.thumbnail = source.readString();
			report.title = source.readString();
			return report;
		}
		
		
		public Report[] newArray(int size) {
			return new Report[size];
		}
	};

}
