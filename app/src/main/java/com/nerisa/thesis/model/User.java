package com.nerisa.thesis.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by nerisa on 3/30/18.
 */

public class User implements Parcelable {

    private Long id;
    private String email;
    private String token;
    private boolean isCustodian;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public User(){}

    public User(Long id, String email, String token, boolean isCustodian) {
        this.id = id;
        this.email = email;
        this.token = token;
        this.isCustodian = isCustodian;
    }

    public User(String email, String token){
        this.email = email;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isCustodian() {
        return isCustodian;
    }

    public void setCustodian(boolean custodian) {
        isCustodian = custodian;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(id);
        parcel.writeString(email);
        parcel.writeByte((byte) (isCustodian ? 1:0));

    }

    private User(Parcel in){
        this.id = in.readLong();
        this.email = in.readString();
        this.isCustodian = (in.readByte() != 0);
    }

    public JSONObject createJsonObjectForServer(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jsonObject;
    }
}
