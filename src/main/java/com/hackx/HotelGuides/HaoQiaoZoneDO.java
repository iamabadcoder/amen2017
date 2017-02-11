package com.hackx.HotelGuides;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class HaoQiaoZoneDO implements Serializable {

    private String zoneNameCh;

    private String zoneNameEn;

    private boolean isHot = false;

    private String zoneDesc;

    private String selectToLivePercent;

    private Integer hotelCount;

    private List<HaoQiaoAttractionsDO> attractionsList = new ArrayList<>();

    public String getZoneNameCh() {
        return zoneNameCh;
    }

    public void setZoneNameCh(String zoneNameCh) {
        this.zoneNameCh = zoneNameCh;
    }

    public String getZoneNameEn() {
        return zoneNameEn;
    }

    public void setZoneNameEn(String zoneNameEn) {
        this.zoneNameEn = zoneNameEn;
    }

    public boolean isHot() {
        return isHot;
    }

    public void setHot(boolean hot) {
        isHot = hot;
    }

    public String getZoneDesc() {
        return zoneDesc;
    }

    public void setZoneDesc(String zoneDesc) {
        this.zoneDesc = zoneDesc;
    }

    public String getSelectToLivePercent() {
        return selectToLivePercent;
    }

    public void setSelectToLivePercent(String selectToLivePercent) {
        this.selectToLivePercent = selectToLivePercent;
    }

    public Integer getHotelCount() {
        return hotelCount;
    }

    public void setHotelCount(Integer hotelCount) {
        this.hotelCount = hotelCount;
    }

    public List<HaoQiaoAttractionsDO> getAttractionsList() {
        return attractionsList;
    }

    public void setAttractionsList(List<HaoQiaoAttractionsDO> attractionsList) {
        this.attractionsList = attractionsList;
    }
}
