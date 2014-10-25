package mil.nga.dice.report;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import android.os.Parcel;
import android.os.Parcelable;

public class Report implements Parcelable {
	private String title;
	private String description;
	private String thumbnail;
	private String filename;
	private String fileExtension;
	private String path;
	private String id;
	private String error;
	private Double lat;
	private Double lon;
	private Boolean enabled;
	
	public Report() {
		super();
	}
	
	public Report (String title) {
		this.title = title;
		this.description = null;
		this.thumbnail = null;
		this.filename = null;
		this.fileExtension = null;
		this.path = null;
		this.id = null;
		this.lat = null;
		this.lon = null;
		this.enabled = false;
		this.error = null;
	}
	
	public Report (String title, String classification, String thumbnail, String filename, String fileExtension, String path, String id, Double lat, Double lon, Boolean enabled, String error) {
		this.title = title;
		this.description = classification;
		this.thumbnail = thumbnail;
		this.filename = filename;
		this.fileExtension = fileExtension;
		this.path = path;
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.enabled = enabled;
		this.error = error;
	}

	public String toString() {
		return this.title;
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

	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getFileExtension() {
		return fileExtension;
	}

	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
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
		if (title != null) 
			parcel.writeString(title);
		else
			parcel.writeString("");
		if (description != null) 
			parcel.writeString(description);
		else
			parcel.writeString("");
		if (thumbnail != null) 
			parcel.writeString(thumbnail);
		else
			parcel.writeString("");
		if (filename != null) 
			parcel.writeString(filename);
		else
			parcel.writeString("");
		if (fileExtension != null) 
			parcel.writeString(fileExtension);
		else
			parcel.writeString("");
		if (path != null) 
			parcel.writeString(path);
		else
			parcel.writeString("");
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
		if (enabled != null)
			parcel.writeByte((byte) (enabled ? 1 : 0));
		else
			parcel.writeByte((byte)0);
	}
	
	
	public static final Parcelable.Creator<Report> CREATOR = new Creator<Report> () {
		@Override
		public Report createFromParcel(Parcel source) {
			Report report = new Report();
			report.title = source.readString();
			report.description = source.readString();
			report.thumbnail = source.readString();
			report.filename = source.readString();
			report.fileExtension = source.readString();
			report.path = source.readString();
			report.error = source.readString();
			report.lat = source.readDouble();
			report.lon = source.readDouble();
			report.enabled = source.readByte() != 0;
			return report;
		}
		
		
		public Report[] newArray(int size) {
			return new Report[size];
		}
	};	
}
