package com.hackx.HotelGuides;

import java.io.Serializable;

public class HaoQiaoAttractionsDO implements Serializable {

    private String attractionsName;

    private String attractionsLatLng;

    public String getAttractionsName() {
        return attractionsName;
    }

    public void setAttractionsName(String attractionsName) {
        this.attractionsName = attractionsName;
    }

    public String getAttractionsLatLng() {
        return attractionsLatLng;
    }

    public void setAttractionsLatLng(String attractionsLatLng) {
        this.attractionsLatLng = attractionsLatLng;
    }
}