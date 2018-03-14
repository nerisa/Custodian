package com.nerisa.thesis.model;

import java.util.Date;

/**
 * Created by nerisa on 3/13/18.
 */

public class Warning {

    private String desc;
    private String image;
    private Date date;
    private boolean isVerified;

    public Warning(){}

    public Warning(String desc, String image, Date date){
        this.desc = desc;
        this.image = image;
        this.date = date;
        this.isVerified = false;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void verify(){
        this.isVerified = true;
    }

    public boolean isVerified(){
        return isVerified;
    }


}
