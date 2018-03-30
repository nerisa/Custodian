package com.nerisa.thesis.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by nerisa on 3/30/18.
 */

public class NoiseData {

    private Long id;
    private String file;
    private Long date;

    public NoiseData(){}

    public NoiseData(String file, Long date) {
        this.file = file;
        this.date = date;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
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

    public JSONObject createJsonToSendToServer(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("file", file);
            jsonObject.put("date", new Date().getTime());
        }catch (JSONException e){
            e.printStackTrace();
        }
        return jsonObject;
    }
}
