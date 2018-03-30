package com.nerisa.thesis.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by nerisa on 3/30/18.
 */

public class TemperatureData implements Parcelable {

    private Long id;
    private Double value;
    private Long date;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public TemperatureData createFromParcel(Parcel in) {
            return new TemperatureData(in);
        }

        public TemperatureData[] newArray(int size) {
            return new TemperatureData[size];
        }
    };

    public TemperatureData(){}

    public TemperatureData( Double value, Long date) {
        this.value = value;
        this.date = date;
    }


    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeDouble(value);
        parcel.writeLong(date);
    }

    private TemperatureData(Parcel in){
        this.id = in.readLong();
        this.value = in.readDouble();
        this.date = in.readLong();
    }

    public JSONObject createJsonToSendToServer(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("value", value);
            jsonObject.put("date", new Date().getTime());
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jsonObject;
    }
}
