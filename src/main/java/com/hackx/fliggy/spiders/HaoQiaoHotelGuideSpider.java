package com.hackx.fliggy.spiders;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HaoQiaoHotelGuideSpider extends TrspCrawlerExtractorAdapter {

    public static final String SEPARATOR_SIGN = "####";

    public static JSONObject extractHotelGuide(Document document) {
        JSONObject jsonObject = null;
        if (document.select("div.zone-title-l").first() == null) {
            return jsonObject;
        }
        jsonObject.put("whereToLive", extractWhereToLiveInfo(document));
        fillZoneInfo(jsonObject, document);
        extractZoneBoundLatlngAndNames(jsonObject, document);

        return jsonObject;
    }

    public static void fillZoneInfo(JSONObject jsonObject, Document document) {
        JSONArray zoneArray = new JSONArray();
        Element jZoneListElement = document.select("ul#J_zone_list").first();
        if (null == jZoneListElement) return;
        for (Element zoneEle : jZoneListElement.children()) {
            JSONObject zoneJSONObject = new JSONObject();
            Element titleEle = zoneEle.select("h4").first();
            if (null != titleEle) {
                Element aEle = titleEle.select("a").first();
                if (null != aEle) {
                    zoneJSONObject.put("zoneNameCh", aEle.ownText());
                    if (null != aEle.select("i").first()) {
                        zoneJSONObject.put("zoneNameEn", aEle.select("i").first().text());
                    }
                    if (null != aEle.select("span").first()) {
                        zoneJSONObject.put("isHot", true);
                    }
                }
            }

            Element zoneListContEle = zoneEle.select("div.zone-list-cont").first();
            if (null != zoneListContEle) {
                StringBuffer sb = new StringBuffer();
                for (Element child : zoneListContEle.children()) {
                    if ("p".equals(child.tagName())) {
                        sb.append(child.text() + "\n");
                    }
                }
                if (!StringUtil.isBlank(sb.toString())) {
                    zoneJSONObject.put("zoneDesc", sb.toString().trim());
                }
            }
            zoneArray.add(zoneJSONObject);
        }
        jsonObject.put("zoneInfo", zoneArray);
    }

    public static String extractWhereToLiveInfo(Document document) {
        Element divZoneTipsContEle = document.select("div.zone-tips-cont").first();
        if (null == divZoneTipsContEle) {
            return null;
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
        return sb.toString().trim();
    }

    public static void extractZoneBoundLatlngAndNames(JSONObject jsonObject, Document document) {
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
                    jsonObject.put("zoneNameList", nameJsonArray);
                    jsonObject.put("zoneBoundList", boundJsonArray);
                }
                break;
            }
        }
    }

    public static void main(String[] args) {
        generateParams();
    }

    public static void generateParams() {
        Map<String, List<String>> siteMap = extractSiteMap("http://www.haoqiao.cn/sitemap.html");
        for (Map.Entry<String, List<String>> entry : siteMap.entrySet()) {
            for (String city : entry.getValue()) {
                String[] countryFields = entry.getKey().split(SEPARATOR_SIGN);
                String[] cityFields = city.split(SEPARATOR_SIGN);
                StringBuffer sb = new StringBuffer();
                sb.append("countryName:'" + countryFields[0] + "',");
                sb.append("countryLink:'" + countryFields[1] + "',");
                sb.append("cityName:'" + cityFields[0] + "',");
                sb.append("url:'" + cityFields[1] + "'");
                System.out.println(sb.toString().trim());
            }
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

    /* 链接格式类似<http://www.haoqiao.cn/mauritius_c666/all.html> 这种形式的链接才会被保留下来*/
    public static List<String> extractCityInfo(Element h1Element) {
        List<String> cityList = new ArrayList<>();
        Element siblingElement = h1Element.nextElementSibling();
        if (null == siblingElement) {
            return cityList;
        }
        for (Element cityEle : siblingElement.select("a")) {
            /*if (cityEle.attr("href").contains("all.html")) {
                cityList.add(cityEle.text() + SEPARATOR_SIGN + cityEle.attr("href"));
            }*/
            cityList.add(cityEle.text() + SEPARATOR_SIGN + cityEle.attr("href"));
        }
        return cityList;
    }

    public static String extractCountryInfo(Element h1Element) {
        Element aEle = h1Element.select("a").first();
        if (aEle != null) {
            return aEle.text() + SEPARATOR_SIGN + aEle.attr("href");
        }
        return null;
    }

    @Override
    protected JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
        Document document = TrspExtractUtils.toDocument(html);
        JSONArray hotelGuideArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", extractHotelGuide(document));
        hotelGuideArray.add(jsonObject);
        return hotelGuideArray;
    }
}
