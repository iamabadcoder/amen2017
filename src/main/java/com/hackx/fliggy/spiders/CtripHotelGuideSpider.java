package com.hackx.fliggy.spiders;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CtripHotelGuideSpider extends TrspCrawlerExtractorAdapter {

    public static final String SEPARATOR_SIGN = "####";

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
        String targetUrl = "http://you.ctrip.com/place/siemreap599.html";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            List<String> zoneInfoList = extractHotelGuide(document);
            JSONArray zoneArray = new JSONArray();
            for (String zone : zoneInfoList) {
                String[] zoneField = zone.split(SEPARATOR_SIGN);
                String patternStr = "(.*)(\\D+)(\\d+%)(.*)";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher m = pattern.matcher(zoneField[0]);
                if (m.find()) {
                    JSONObject zoneObject = new JSONObject();
                    zoneObject.put("zoneGroupName", m.group(1) + m.group(2));
                    zoneObject.put("zoneGroupRatio", m.group(3));
                    zoneArray.add(zoneObject);
                }
            }
        } catch (Exception e) {

        }
        /*generateParams();*/
    }


    public static void simulateTest() {

    }

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

}
