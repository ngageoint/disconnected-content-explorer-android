package mil.nga.dice.report;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;

public class Report implements Parcelable {
	private File sourceFile;
	private File path;
	private String title;
	private String description;
	private String thumbnail;
	private String id;
	private String error;
	private Double lat;
	private Double lon;
	private Boolean enabled = false;


	public Report() {}


	public String toString() {
		return this.title;
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(File x) {
		sourceFile = x;
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

	public String getFileName() {
		return sourceFile.getName();
	}
	
	public String getFileExtension() {
		String fileName = getFileName();
		int lastDot = fileName.lastIndexOf(".");
		if (lastDot > -1 && lastDot < fileName.length() - 1) {
			return fileName.substring(lastDot + 1);
		}
		return null;
	}

	/**
	 * The absolute path to the report content.
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
	
	public void setEnabled (Boolean enabled) {
		this.enabled = enabled;
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
			parcel.writeString("");

		if (enabled != null)
			parcel.writeByte((byte) (enabled ? 1 : 0));
		else
			parcel.writeByte((byte)0);

		if (error != null)
			parcel.writeString(error);
		else
			parcel.writeString("");

		if (lat != null)
			parcel.writeDouble(lat);
		else
			parcel.writeDouble(0.0);

		if (lon != null)
			parcel.writeDouble(lon);
		else
			parcel.writeDouble(0.0);

		if (path != null)
			parcel.writeString(path.getAbsolutePath());
		else
			parcel.writeString("");

		if (sourceFile != null)
			parcel.writeString(sourceFile.getAbsolutePath());
		else
			parcel.writeString("");

		if (thumbnail != null)
			parcel.writeString(thumbnail);
		else
			parcel.writeString("");

		if (title != null)
			parcel.writeString(title);
		else
			parcel.writeString("");

	}

	public static final Parcelable.Creator<Report> CREATOR = new Creator<Report> () {
		@Override
		public Report createFromParcel(Parcel source) {
			Report report = new Report();
			report.description = source.readString();
			report.enabled = source.readByte() != 0;
			report.error = source.readString();
			report.lat = source.readDouble();
			report.lon = source.readDouble();
			Object value;
			value = source.readString();
			if (!value.toString().isEmpty()) {
				report.path = new File(value.toString());
			}
			value = source.readString();
			if (!value.toString().isEmpty()) {
				report.sourceFile = new File(value.toString());
			}
			report.thumbnail = source.readString();
			report.title = source.readString();
			return report;
		}
		
		
		public Report[] newArray(int size) {
			return new Report[size];
		}
	};

}
