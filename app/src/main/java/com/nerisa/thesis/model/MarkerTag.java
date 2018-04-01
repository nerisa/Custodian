package com.nerisa.thesis.model;

/**
 * Created by nerisa on 3/31/18.
 */

public class MarkerTag {

    Long monumentId;
    String wikiId;
    MarkerType type;
    String wikiName;

    public MarkerTag(Long monumentId) {
        this.monumentId = monumentId;
        this.type = MarkerType.MONUMENT;
    }

    public MarkerTag(String wikiId, String wikiString) {
        this.wikiId = wikiId;
        this.wikiName = wikiString;
        this.type = MarkerType.WIKI;
    }

    public Long getMonumentId() {
        return monumentId;
    }

    public void setMonumentId(Long monumentId) {
        this.monumentId = monumentId;
    }

    public String getWikiId() {
        return wikiId;
    }

    public void setWikiId(String wikiId) {
        this.wikiId = wikiId;
    }

    public MarkerType getType() {
        return type;
    }

    public void setType(MarkerType type) {
        this.type = type;
    }

    public String getWikiName() {
        return wikiName;
    }

    public void setWikiName(String wikiName) {
        this.wikiName = wikiName;
    }

    public enum MarkerType{
        WIKI, MONUMENT
    }
}
