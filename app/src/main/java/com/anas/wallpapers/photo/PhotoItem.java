package com.anas.wallpapers.photo;

import android.os.Parcel;
import android.os.Parcelable;

public class PhotoItem implements Parcelable {
    private int column;
    private String gid;
    private int id;
    private String monthDescription;
    private String photoCode;
    private PhotoInfo photoInfo;
    private long scheduleTime;

    public PhotoItem(String photoCode, int id, long scheduleTime, String gid, PhotoInfo photoInfo, String monthDescription, int column) {
        this.photoCode = photoCode;
        this.id = id;
        this.scheduleTime = scheduleTime;
        this.gid = gid;
        this.photoInfo = photoInfo;
        this.monthDescription = monthDescription;
        this.column = column;
    }
    public PhotoItem(Parcel in) {
        column = in.readInt();
        id = in.readInt();
        scheduleTime = in.readLong();
        gid = in.readString();
        photoCode = in.readString();
        photoInfo = (PhotoInfo) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int arg1) {
        dest.writeInt(column);
        dest.writeInt(id);
        dest.writeLong(scheduleTime);
        dest.writeString(gid);
        dest.writeString(photoCode);
        dest.writeSerializable(photoInfo);
    }

    public long getScheduleTime() {
        return scheduleTime;
    }

    public String getMonthDescription() {
        return monthDescription;
    }

    public int getColumn() {
        return column;
    }

    public PhotoInfo getPhotoInfo() {
        return this.photoInfo;
    }

    public static final Parcelable.Creator<PhotoItem> CREATOR = new Parcelable.Creator<PhotoItem>() {
        public PhotoItem createFromParcel(Parcel in) {
            return new PhotoItem(in);
        }

        public PhotoItem[] newArray(int size) {
            return new PhotoItem[size];
        }
    };
}
