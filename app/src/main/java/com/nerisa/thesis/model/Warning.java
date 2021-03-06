package com.nerisa.thesis.model;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by nerisa on 3/13/18.
 */

public class Warning implements Parcelable {

    private long id;
    private String desc;
    private String image;
    private long date;
    private boolean verified;
    private long userId;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Warning createFromParcel(Parcel in) {
            return new Warning(in);
        }

        public Warning[] newArray(int size) {
            return new Warning[size];
        }
    };

    public Warning(){}

    public Warning(String desc, String image, long date){
        this.desc = desc;
        this.image = image;
        this.date = date;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setVerify(boolean isVerified){
        this.verified = isVerified;
    }

    public boolean isVerified(){
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(desc);
        parcel.writeString(image);
        parcel.writeLong(date);
        parcel.writeLong(userId);
        parcel.writeByte((byte) (verified ? 1 : 0));
        parcel.writeLong(id);
    }

    private Warning(Parcel in){
        this.desc = in.readString();
        this.image = in.readString();
        this.date = in.readLong();
        this.userId = in.readLong();
        this.verified = (in.readByte() != 0);
        this.id = in.readLong();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
