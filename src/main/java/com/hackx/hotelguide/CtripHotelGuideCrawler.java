package com.hackx.hotelguide;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CtripHotelGuideCrawler {

    public static final String SEPARATOR_SIGN = "####";

    public static void main(String[] args) throws Exception {
        String countryListUrl = "http://you.ctrip.com/place/countrylist.html";
        Map<String, String> placesMap = extractAllPlaces(countryListUrl);
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, String> entry : placesMap.entrySet()) {
            String[] cityFields = entry.getKey().split(SEPARATOR_SIGN);
            jsonObject.put("cityNameCh", cityFields[0]);
            jsonObject.put("cityNameEn", cityFields.length == 2 ? cityFields[1] : null);
            jsonObject.put("cityLink", entry.getValue());
            Thread.sleep(3000); /* 延缓3秒防止被封 */
            System.out.println(cityFields[0]);
            List<String> zoneInfoList = extractZoneInfo(entry.getValue());
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
            jsonObject.put("zoneInfoList", zoneArray);
            write2File("CtripHotelGuideData.txt", jsonObject.toJSONString());
        }
    }

    public static Map<String, List<String>> extractZoneCoordinateAndSight(String zoneUrl) {
        Map<String, List<String>> zoneCoordinateSightMap = new HashMap<>();
        try {
            Thread.sleep(3000);/* 延缓3秒防止被封 */
            Document document = Jsoup.connect(zoneUrl).get();

            Element areaElement = document.select("dd#Area").first();
            if (null == areaElement) return zoneCoordinateSightMap;
            StringBuffer sbKey = new StringBuffer();
            for (Element aEle : areaElement.select("a")) {
                if (aEle.attr("href").contains(zoneUrl.replace("http://you.ctrip.com", "").replace(".html", ""))) {
                    sbKey.append(aEle.text().trim()).append(SEPARATOR_SIGN);
                    sbKey.append(aEle.attr("data-lat").trim()).append(SEPARATOR_SIGN);
                    sbKey.append(aEle.attr("data-lon").trim());
                    zoneCoordinateSightMap.put(sbKey.toString(), null);
                }
            }

            Element sightElement = document.select("dl#Sight").first();
            if (null == sightElement) return zoneCoordinateSightMap;
            List<String> sightList = new ArrayList<>();
            for (Element aEle : areaElement.select("a")) {
                if (aEle.ownText().trim().equals("不限")) {
                    continue;
                }
                StringBuffer sbValue = new StringBuffer();
                sbValue.append(aEle.ownText().trim()).append(SEPARATOR_SIGN);
                sbValue.append(aEle.attr("data-lat").trim()).append(SEPARATOR_SIGN);
                sbValue.append(aEle.attr("data-lon").trim());
                sightList.add(sbValue.toString());
            }
            zoneCoordinateSightMap.put(sbKey.toString(), sightList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zoneCoordinateSightMap;
    }

    /*
    * 返回数据格式:
    * [人民广场25%选择####http://you.ctrip.com/hotels/shanghai2/list-100690.html,
    *  豫园24%选择####http://you.ctrip.com/hotels/shanghai2/list-100690.html,
    *  淮海路15%选择####http://you.ctrip.com/hotels/shanghai2/list-100692.html]
    * */
    public static List<String> extractZoneInfo(String placeUrl) {
        List<String> hotelGuideInfoList = new ArrayList<>();
        try {
            Document document = Jsoup.connect(placeUrl).get();

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

    /*
    * 返回数据格式:
    * {杭州####Hangzhou=http://you.ctrip.com/place/hangzhou14.html,
    *  上海####Shanghai=http://you.ctrip.com/place/shanghai2.html}
    * */
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

    public static void write2File(String fileName, String content) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true), "UTF-8"));
            writer.write(content + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
