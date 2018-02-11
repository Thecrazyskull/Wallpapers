package com.anas.wallpapers.photo;

import android.util.Log;

import com.anas.wallpapers.utils.Utilities;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PhotoInfo implements Serializable {
    private String apertureValue;
    private String author;
    private String countryCode;
    private String dateTimeOriginal;
    private String email;
    private String focalLength;
    private String homepage;
    private String iSOSpeedRatings;
    private int imageHeight;
    private int imageWidth;
    private String make;
    private String model;
    private String photoTopic;
    private String photoUrl;
    private String shutterSpeedValue;
    private String uploadLocation;
    private long uploadTime;

    private String date;
    private String thumnailUrl;

    public String getShutterSpeedValue() {
        return shutterSpeedValue;
    }

    public String getApertureValue() {
        return apertureValue;
    }

    public String getDateTimeOriginal() {
        return dateTimeOriginal;
    }

    public String getAuthor() {
        return author;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public String getCountryName() {
        return Utilities.getCountryName(countryCode);
    }

    public String getHomepage() {
        return homepage;
    }

    public String getUploadLocation() {
        return uploadLocation;
    }

    public String getISOSpeedRatings() {
        return iSOSpeedRatings;
    }

    public String getMake() {
        return make;
    }

    public Integer getImageWidth() {
        return 1920;
    }

    public Integer getImageHeight() {
        return 1080;
    }

    public String getPhotoTopic() {
        return photoTopic;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getThumbnailUrl() {
        return thumnailUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getFocalLength() {
        return focalLength;
    }

    public String getModel() {
        return model;
    }

    public String getDate() {
        return date;
    }

    public void setDate(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        String dateTime = Long.toString(time);
        try {
            Date d = format.parse(dateTime);
            format = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
            date = format.format(d);
        } catch (ParseException e) {
            Log.e("PhotoInfo", "failed to parse date ", e);
        }
    }

    public void setThumbnailUrl(String url) {
        thumnailUrl = url;
    }
}
