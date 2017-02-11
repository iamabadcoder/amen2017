package com.hackx.HotelGuides;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hackx on 2/11/17.
 */
public class HaoQiaoHotelGuideDO implements Serializable {

    private String contryName;

    private String contryLink;

    private String cityName;

    private String cityLink;

    private String whereToLive;

    private List<HaoQiaoZoneDO> zoneDOList = new ArrayList<>();

    public String getContryName() {
        return contryName;
    }

    public void setContryName(String contryName) {
        this.contryName = contryName;
    }

    public String getContryLink() {
        return contryLink;
    }

    public void setContryLink(String contryLink) {
        this.contryLink = contryLink;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getCityLink() {
        return cityLink;
    }

    public void setCityLink(String cityLink) {
        this.cityLink = cityLink;
    }

    public String getWhereToLive() {
        return whereToLive;
    }

    public void setWhereToLive(String whereToLive) {
        this.whereToLive = whereToLive;
    }

    public List<HaoQiaoZoneDO> getZoneDOList() {
        return zoneDOList;
    }

    public void setZoneDOList(List<HaoQiaoZoneDO> zoneDOList) {
        this.zoneDOList = zoneDOList;
    }
}
