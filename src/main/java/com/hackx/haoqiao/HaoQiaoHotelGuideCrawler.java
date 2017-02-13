package com.hackx.haoqiao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.hackx.HotelGuides.HaoQiaoAttractionsDO;
import com.hackx.HotelGuides.HaoQiaoHotelGuideDO;
import com.hackx.HotelGuides.HaoQiaoZoneDO;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HaoQiaoHotelGuideCrawler {

    public static final String SEPARATOR_SIGN = "####";

    public static void main(String[] args) {
        Map<String, List<String>> siteMap = extractSiteMap("http://www.haoqiao.cn/sitemap.html");
        System.out.println(siteMap.values().stream().map((city) -> city.size()).reduce((a, b) -> a + b).get());
        for (Map.Entry<String, List<String>> entry : siteMap.entrySet()) {
            for (String city : entry.getValue()) {
                HaoQiaoHotelGuideDO haoQiaoHotelGuideDO = new HaoQiaoHotelGuideDO();
                if (StringUtil.isBlank(haoQiaoHotelGuideDO.getCityName())) {
                    fillCountryInfo(haoQiaoHotelGuideDO, entry.getKey());
                }
                fillCityInfo(haoQiaoHotelGuideDO, city);
                try {
                    Document document = Jsoup.connect("http://www.haoqiao.cn/kaohsiung_c12/all.html").get();
                    /*Document document = Jsoup.connect(haoQiaoHotelGuideDO.getCityLink()).get();*/
                    fillWhereToLiveInfo(haoQiaoHotelGuideDO, document);
                    fillZoneInfo(haoQiaoHotelGuideDO, document);
                    System.out.println(JSON.toJSON(haoQiaoHotelGuideDO));
                    extractZoneBoundLatlngAndNames(document);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<Map<String, String>> extractZoneBoundLatlngAndNames(Document document) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Element scriptEle : document.select("script[type='text/javascript']")) {
            if (scriptEle.data().contains("var bound_latlng =")) {
                JSONArray boundJsonArray = null;
                JSONArray nameJsonArray = null;
                String[] zoneScriptInfos = scriptEle.data().split(";");
                for (String zsi : zoneScriptInfos) {
                    if (zsi.contains("var bound_latlng =")) {
                        boundJsonArray = JSON.parseArray(zsi.replace("var bound_latlng =", "").trim());
                    } else if (zsi.contains("var name_d = ")) {
                        nameJsonArray = JSON.parseArray(zsi.replace("var name_d = ", "").trim());
                    }
                }
                if (null != boundJsonArray && null != nameJsonArray && boundJsonArray.size() == nameJsonArray.size()) {
                    for (int i = 0; i < nameJsonArray.size(); i++) {
                        Map<String, String> map = new HashMap<String, String>();
                        map.put(nameJsonArray.get(0).toString(), boundJsonArray.get(0).toString());
                        list.add(map);
                    }
                }
                break;
            }
        }
        return list;
    }

    public static void fillZoneInfo(HaoQiaoHotelGuideDO haoQiaoHotelGuideDO, Document document) {
        Element jZoneListElement = document.select("ul#J_zone_list").first();
        if (null == jZoneListElement) return;
        for (Element zoneEle : jZoneListElement.children()) {
            HaoQiaoZoneDO haoQiaoZoneDO = new HaoQiaoZoneDO();

            Element titleEle = zoneEle.select("h4").first();
            if (null != titleEle) {
                Element aEle = titleEle.select("a").first();
                if (null != aEle) {
                    haoQiaoZoneDO.setZoneNameCh(aEle.ownText());
                    if (null != aEle.select("i").first()) {
                        haoQiaoZoneDO.setZoneNameEn(aEle.select("i").first().text());
                    }
                    if (null != aEle.select("span").first()) {
                        haoQiaoZoneDO.setHot(true);
                    }
                }
            }

            Element zoneListContEle = zoneEle.select("div.zone-list-cont").first();
            if (null != zoneListContEle) {
                StringBuffer sb = new StringBuffer();
                for (Element child : zoneListContEle.children()) {
                    if ("p".equals(child.tagName())) {
                        sb.append(child.text() + "\n");
                    } else if (child.hasClass("zone-main-attraction")) {
                        for (Element tokenEle : child.select("a.J_token")) {
                            HaoQiaoAttractionsDO haoQiaoAttractionsDO = new HaoQiaoAttractionsDO();
                            haoQiaoAttractionsDO.setAttractionsName(tokenEle.ownText());
                            haoQiaoAttractionsDO.setAttractionsLatLng(tokenEle.attr("data-latlng"));
                            haoQiaoZoneDO.getAttractionsList().add(haoQiaoAttractionsDO);
                        }
                    }
                }
                if (!StringUtil.isBlank(sb.toString())) {
                    haoQiaoZoneDO.setZoneDesc(sb.toString().trim());
                    String pattern = "\\d+家酒店";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(sb.toString());
                    if (m.find()) {
                        haoQiaoZoneDO.setHotelCount(Integer.valueOf(m.group().replace("家酒店", "")));
                    }
                }
            }

            Element liveRatioEle = zoneListContEle.select("span.zone-l-live-ratio").first();
            if (null != liveRatioEle) {
                haoQiaoZoneDO.setSelectToLivePercent(liveRatioEle.text());
            }
            haoQiaoHotelGuideDO.getZoneDOList().add(haoQiaoZoneDO);
        }
    }

    public static void fillWhereToLiveInfo(HaoQiaoHotelGuideDO haoQiaoHotelGuideDO, Document document) {
        Element divZoneTipsContEle = document.select("div.zone-tips-cont").first();
        if (null == divZoneTipsContEle) {
            return;
        }
        Elements pElements = divZoneTipsContEle.select("p");
        StringBuffer sb = new StringBuffer();
        for (Element pEle : pElements) {
            if (pEle.text().contains("跳过，查看全部酒店")) {
                break;
            } else {
                sb.append(pEle.text() + "\n");
            }
        }
        haoQiaoHotelGuideDO.setWhereToLive(sb.toString().trim());
    }

    public static void fillCountryInfo(HaoQiaoHotelGuideDO haoQiaoHotelGuideDO, String countryInfo) {
        String[] fields = countryInfo.split(SEPARATOR_SIGN);
        if (fields.length == 2) {
            haoQiaoHotelGuideDO.setContryName(fields[0]);
            haoQiaoHotelGuideDO.setContryLink(fields[1]);
        }
    }

    public static void fillCityInfo(HaoQiaoHotelGuideDO haoQiaoHotelGuideDO, String cityInfo) {
        String[] fields = cityInfo.split(SEPARATOR_SIGN);
        if (fields.length == 2) {
            haoQiaoHotelGuideDO.setCityName(fields[0]);
            haoQiaoHotelGuideDO.setCityLink(fields[1]);
        }
    }

    public static Map<String, List<String>> extractSiteMap(String siteMapUrl) {
        Map<String, List<String>> siteMap = new HashMap<>();
        try {
            Document document = Jsoup.connect(siteMapUrl).get();
            Element divSiteMapEle = document.select("div.sitemap").first();
            if (null == divSiteMapEle) {
                return siteMap;
            }
            for (Element childEle : divSiteMapEle.children()) {
                if ("h1".equals(childEle.tagName())) {
                    String countryName = extractCountryInfo(childEle);
                    List<String> cityList = extractCityInfo(childEle);
                    if (!StringUtil.isBlank(countryName) && cityList.size() > 0) {
                        siteMap.put(countryName, cityList);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return siteMap;
    }

    public static String extractCountryInfo(Element h1Element) {
        Element aEle = h1Element.select("a").first();
        if (aEle != null) {
            return aEle.text() + SEPARATOR_SIGN + aEle.attr("href");
        }
        return null;
    }

    /* 链接格式类似<http://www.haoqiao.cn/mauritius_c666/all.html> 这种形式的链接才会被保留下来*/
    public static List<String> extractCityInfo(Element h1Element) {
        List<String> cityList = new ArrayList<>();
        Element siblingElement = h1Element.nextElementSibling();
        if (null == siblingElement) {
            return cityList;
        }
        for (Element cityEle : siblingElement.select("a")) {
            if (cityEle.attr("href").contains("all.html")) {
                cityList.add(cityEle.text() + SEPARATOR_SIGN + cityEle.attr("href"));
            }
        }
        return cityList;
    }

}
