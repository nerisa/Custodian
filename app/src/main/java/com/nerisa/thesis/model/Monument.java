package com.nerisa.thesis.model;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by nerisa on 2/12/18.
 */

public class Monument implements Parcelable {

    private String name;
    private String desc;
    private double longitude;
    private double latitude;
    private String builder;
    private String monumentPhoto;
    private String noiseRecording;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Monument createFromParcel(Parcel in) {
            return new Monument(in);
        }

        public Monument[] newArray(int size) {
            return new Monument[size];
        }
    };

    public Monument(){}

    private Monument(Parcel in){
        this.name = in.readString();
        this.desc = in.readString();
        this.builder = in.readString();
        this.longitude = in.readDouble();
        this.latitude = in.readDouble();
    }

    public Monument(String name, String desc, double longitude, double latitude, String creator){
        this.name = name;
        this.desc = desc;
        this.longitude = longitude;
        this.latitude = latitude;
        this.builder = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getBuilder() {
        return builder;
    }

    public void setBuilder(String builder) {
        this.builder = builder;
    }

    public String getMonumentPhoto() {
        return monumentPhoto;
    }

    public void setMonumentPhoto(String monumentPhoto) {
        this.monumentPhoto = monumentPhoto;
    }

    public String getNoiseRecording() {
        return noiseRecording;
    }

    public void setNoiseRecording(String noiseRecording) {
        this.noiseRecording = noiseRecording;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(desc);
        parcel.writeString(builder);
        parcel.writeDouble(longitude);
        parcel.writeDouble(latitude);
    }
}
