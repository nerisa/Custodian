package com.nerisa.thesis.model;

import java.util.Date;

/**
 * Created by nerisa on 3/14/18.
 */

public class Post {

    private String desc;
    private Long date;

    public Post(){}

    public Post(String desc, Long date){
        this.desc = desc;
        this.date = date;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }
}
