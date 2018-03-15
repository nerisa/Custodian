package com.nerisa.thesis.model;

import android.os.Parcel;
import android.os.Parcelable;


/**
 * Created by nerisa on 3/13/18.
 */

public class Warning implements Parcelable {

    private String desc;
    private String image;
    private Long date;
    private boolean isVerified;

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Warning createFromParcel(Parcel in) {
            return new Warning(in);
        }

        public Warning[] newArray(int size) {
            return new Warning[size];
        }
    };

    public Warning(){}

    public Warning(String desc, String image, Long date){
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

    public void setDate(Long date) {
        this.date = date;
    }

    public void setVerify(boolean isVerified){
        this.isVerified = isVerified;
    }

    public boolean isVerified(){
        return isVerified;
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
    }

    private Warning(Parcel in){
        this.desc = in.readString();
        this.image = in.readString();
        this.date = in.readLong();
    }

}
