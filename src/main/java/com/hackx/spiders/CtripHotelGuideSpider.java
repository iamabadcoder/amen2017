package com.hackx.spiders;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CtripHotelGuideSpider extends TrspCrawlerExtractorAdapter {

    public static final String SEPARATOR_SIGN = "####";

    @Override
    protected JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
        Document document = TrspExtractUtils.toDocument(html);
        JSONArray hotelGuideArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();

        List<String> zoneInfoList = extractHotelGuide(document);
        JSONArray zoneArray = new JSONArray();
        for (String zone : zoneInfoList) {
            String[] zoneField = zone.split(SEPARATOR_SIGN);
            String patternStr = "(\\D*)(\\d+%)(.*)";
            Pattern pattern = Pattern.compile(patternStr);
            Matcher m = pattern.matcher(zoneField[0]);
            if (m.find()) {
                JSONObject zoneObject = new JSONObject();
                zoneObject.put("zoneGroupName", m.group(1));
                zoneObject.put("zoneGroupRatio", m.group(2));
                zoneArray.add(zoneObject);
            }
        }
        jsonObject.put("data", zoneArray);
        hotelGuideArray.add(jsonObject);
        return hotelGuideArray;
    }

    public static List<String> extractHotelGuide(Document document) {
        List<String> hotelGuideInfoList = new ArrayList<>();
        try {
            Element hotelWrapEle = document.select("div.hotelwrap").first();
            if (null == hotelWrapEle) return hotelGuideInfoList;

            Element normalTitleEle = hotelWrapEle.select("div.normaltitle").first();
            if (null == normalTitleEle) return hotelGuideInfoList;

            Element percentEle = normalTitleEle.select("span.percent").first();
            if (null == percentEle) return hotelGuideInfoList;

            for (Element aEle : percentEle.select("a")) {
                StringBuffer sb = new StringBuffer();
                sb.append(aEle.text().trim()).append(SEPARATOR_SIGN);
                sb.append("http://you.ctrip.com" + aEle.attr("href"));
                hotelGuideInfoList.add(sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hotelGuideInfoList;
    }

    public static void main(String[] args) {
        generateParams();
    }

    public static void generateParams() {
        String countryListUrl = "http://you.ctrip.com/place/countrylist.html";
        Map<String, String> placesMap = extractAllPlaces(countryListUrl);
        for (Map.Entry<String, String> entry : placesMap.entrySet()) {
            String[] cityFields = entry.getKey().split(SEPARATOR_SIGN);
            StringBuffer sb = new StringBuffer();
            sb.append("cityName:'" + cityFields[0] + "',");
            /*sb.append("cityNameEn:'" + cityFields[1] + "',");*/
            sb.append("url:'" + entry.getValue() + "'");
            System.out.println(sb.toString().trim());
        }
    }

    public static Map<String, String> extractAllPlaces(String countryListUrl) {
        Map<String, String> placesMap = new HashMap<>();
        try {
            Document document = Jsoup.connect(countryListUrl).get();
            for (Element countrylistEle : document.select("div.countrylist")) {
                for (Element liEle : countrylistEle.select("li")) {
                    if (null != liEle.select("a").first()) {
                        String placeName = liEle.select("a").first().text().trim();
                        String placeLink = "http://you.ctrip.com" + liEle.select("a").first().attr("href").trim();
                        if (null != liEle.select("span").first()) {
                            placeName = placeName + SEPARATOR_SIGN + liEle.select("span").first().text().trim();
                        }
                        placesMap.put(placeName, placeLink);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return placesMap;
    }

}
