package com.nerisa.thesis.model;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nerisa.thesis.constant.Constant;
import com.nerisa.thesis.util.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private long userId;
    private String reference;

    private static final String TAG = Monument.class.getSimpleName();

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Monument createFromParcel(Parcel in) {
            return new Monument(in);
        }

        public Monument[] newArray(int size) {
            return new Monument[size];
        }
    };

    public Monument(){}


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
        parcel.writeString(monumentPhoto);
        parcel.writeLong(id);
        parcel.writeLong(userId);
        parcel.writeString(reference);
    }

    private Monument(Parcel in){
        this.name = in.readString();
        this.desc = in.readString();
        this.creator = in.readString();
        this.longitude = in.readDouble();
        this.latitude = in.readDouble();
        this.monumentPhoto = in.readString();
        this.id = in.readLong();
        this.userId = in.readLong();
        this.reference = in.readString();
    }

    public JSONObject createJsonToSendToServer(){
        JSONObject jsonObj = new JSONObject();
        TemperatureData temperatureData = new TemperatureData(temperature, new Date().getTime());
        JSONArray temperatureList = new JSONArray();
        temperatureList.put(temperatureData.createJsonToSendToServer());

        NoiseData noiseData = new NoiseData(noiseRecording, new Date().getTime());
        JSONArray noiseDataList = new JSONArray();
        noiseDataList.put(noiseData.createJsonToSendToServer());

        User user = new User();
        user.setId(userId);

        try {
            jsonObj.put("name", name);
            jsonObj.put("desc", desc);
            jsonObj.put("creator", creator);
            jsonObj.put("longitude", longitude);
            jsonObj.put("latitude", latitude);
            jsonObj.put("monumentPhoto", monumentPhoto);
            jsonObj.put("temperatures", temperatureList);
            jsonObj.put("noiseProfiles", noiseDataList);
            jsonObj.put("custodian", user.createJsonObjectForServer());
            jsonObj.put("reference", reference);
        } catch (JSONException e){
            e.printStackTrace();
        }
        return jsonObj;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public static Monument mapResponse(JSONObject response){
        Log.d(TAG, "Mapping response for " + response.toString());
        Monument monument = new Monument();
        try {
            monument.setId(response.getLong("id"));
            monument.setLatitude(response.getDouble("latitude"));
            monument.setLongitude(response.getDouble("longitude"));
            monument.setName(response.getString("name"));
            monument.setDesc(response.getString("desc"));
            monument.setMonumentPhoto(response.getString("monumentPhoto"));
            JSONObject user = response.getJSONObject("custodian");
            monument.setUserId(user.getLong("id"));
            if(response.has("reference")){
                monument.setReference(response.getString("reference"));
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return monument;
    }

    public static Monument mapWikiResponse(JSONObject response){
        Log.d(TAG, "Mapping wiki reponse: "+ response.toString());
        Monument monument = new Monument();
        try{
            monument.setLongitude(response.getDouble("lon"));
            monument.setLatitude(response.getDouble("lat"));
            monument.setName(response.getString("title"));
            monument.setReference(String.format(Constant.WIKI_PAGE_URL, response.getLong("pageid")));
            //TODO page id
        }catch (JSONException e){
            Log.e(TAG, "Error parsing wiki response " + e.getStackTrace());
        }
        return monument;
    }

    public static Monument mapWikiDetailedResponse(JSONObject response){
        Log.d(TAG, "Mapping detailed response from wiki: " + response.toString());
        Monument monument = new Monument();
        try{
            monument.setName(response.getString("displaytitle"));
            monument.setLatitude(response.getJSONObject("coordinates").getDouble("lat"));
            monument.setLongitude(response.getJSONObject("coordinates").getDouble("lon"));
            monument.setDesc(response.getString("extract"));
            monument.setReference(String.format(Constant.WIKI_PAGE_URL, response.getLong("pageid")));
            monument.setMonumentPhoto(response.getJSONObject("originalimage").getString("source"));
        }catch (JSONException e){
            Log.e(TAG, "Error parsing wiki response" + e.getStackTrace());
        }
        return monument;
    }



    @Override
    public boolean equals(Object anotherMonument){
        if (anotherMonument == null)
                return Boolean.FALSE;
        if(anotherMonument instanceof Monument) {
            Monument comparedMonument = (Monument) anotherMonument;
            if (this.reference == null || this.reference == "" || comparedMonument.getReference() == null || comparedMonument.getReference() == "") {
                return Boolean.FALSE;
            } else {
                boolean result = this.reference.equalsIgnoreCase(comparedMonument.getReference());
                System.out.println("result: " + result);
                return (result);
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (reference == null ? 0 : reference.hashCode());
        return result;
    }
}