package com.nerisa.thesis.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by nerisa on 2/12/18.
 */

public class Monument implements Parcelable {

    private long id;
    private String name;
    private String desc;
    private double longitude;
    private double latitude;
    private String creator;
    private String monumentPhoto;
    private String noiseRecording;

    private Double temperature;

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
        this.creator = in.readString();
        this.longitude = in.readDouble();
        this.latitude = in.readDouble();
        this.temperature = in.readDouble();
        this.id = in.readLong();
    }

    public Monument(String name, String desc, double longitude, double latitude, String creator, double temperature){
        this.name = name;
        this.desc = desc;
        this.longitude = longitude;
        this.latitude = latitude;
        this.creator = creator;
        this.temperature = temperature;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
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

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(desc);
        parcel.writeString(creator);
        parcel.writeDouble(longitude);
        parcel.writeDouble(latitude);
    }
}
