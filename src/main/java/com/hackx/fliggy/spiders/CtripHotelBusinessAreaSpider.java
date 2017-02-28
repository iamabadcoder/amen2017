package com.hackx.fliggy.spiders;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.downloader.domain.exception.TrspDownloadException;
import com.alibaba.trip.tripspider.extractor.domain.TrspExtractResultDO;
import com.alibaba.trip.tripspider.extractor.domain.exception.TrspExtractException;
import com.alibaba.trip.tripspider.httpclient.domain.TrspHttpRequestParam;
import com.alibaba.trip.tripspider.httpclient.domain.enumerate.TrspHttpMethod;
import com.alibaba.trip.tripspider.httpclient.domain.enumerate.TrspProxyType;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerAdapter;
import com.alibaba.trip.tripspider.spider.domain.TrspSpiderJobParamDO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CtripHotelBusinessAreaSpider extends TrspCrawlerAdapter {
    public static void main(String[] args) {
        /*generateParams();*/

        String businessAreaDesc = "西湖47%选择";
        String patternStr = "(\\d+%)";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher m = pattern.matcher(businessAreaDesc);
        if (m.find()) {
            String ratio = m.group(1);
            String businessAreaName = businessAreaDesc.substring(0, businessAreaDesc.indexOf(ratio));
        }

    }

    public static void generateParams() {
        String countryListUrl = "http://you.ctrip.com/place/countrylist.html";
        try {
            Document document = Jsoup.connect(countryListUrl).get();
            for (Element countrylistEle : document.select("div.countrylist")) {
                for (Element liEle : countrylistEle.select("li")) {
                    if (null != liEle.select("a").first()) {
                        String placeName = liEle.select("a").first().text().trim();
                        String placeLink = "http://you.ctrip.com" + liEle.select("a").first().attr("href").trim();
                        System.out.println("placeName:'" + placeName + "',placeLink:'" + placeLink + "'");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public TrspExtractResultDO crawl(TrspSpiderJobParamDO trspSpiderJobParamDO) throws TrspExtractException, TrspDownloadException, Exception {
        TrspExtractResultDO trspExtractResultDO = new TrspExtractResultDO();

        /* 抓取结果 */
        JSONObject jsonObject = new JSONObject();

        /* 参数获取 */
        JSONObject crawlParam = trspSpiderJobParamDO.getParam();
        String placeName = crawlParam.getString("placeName");
        String placeLink = crawlParam.getString("placeLink");
        jsonObject.put("placeName", placeName);
        jsonObject.put("placeLink", placeLink);

        List<Map<String, String>> businessAreaList = new ArrayList<>();
        List<Map<String, List<String>>> hotelTitleList = new ArrayList<>();

        /* 商圈信息抽取 */
        TrspHttpRequestParam trspHttpRequestParam = buildRequestParam();
        trspHttpRequestParam.setUrl(placeLink);
        String cityDetailResponseBody = trspHttpManager.request(trspHttpRequestParam).getBody();
        businessAreaList = crawlBusinessAreas(Jsoup.parse(cityDetailResponseBody));
        jsonObject.put("businessAreaList", businessAreaList);

        /* 酒店列表抽取 */
        for (Map<String, String> map : businessAreaList) {
            String targetUrl = map.get("businessAreaLink");
            trspHttpRequestParam.setUrl(targetUrl);
            String businessAreaResponseBody = trspHttpManager.request(trspHttpRequestParam).getBody();
            List<String> hotelTitles = crawlHotelInfo(Jsoup.parse(businessAreaResponseBody));
            Map<String, List<String>> hotelTitleMap = new HashMap<>();
            hotelTitleMap.put(map.get("businessAreaName"), hotelTitles);
            hotelTitleList.add(hotelTitleMap);
        }

        jsonObject.put("hotelTitleList", hotelTitleList);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        trspExtractResultDO.setResult(jsonArray);
        return trspExtractResultDO;
    }

    public List<String> crawlHotelInfo(Document document) {
        List<String> hotelInfoList = new ArrayList<>();
        for (Element rdetailboxEle : document.select("div.rdetailbox")) {
            Element aEle = rdetailboxEle.select("a").first();
            if (null != aEle) {
                hotelInfoList.add(aEle.ownText());
            }
        }
        return hotelInfoList;
    }

    public List<Map<String, String>> crawlBusinessAreas(Document document) {
        List<Map<String, String>> businessAreaInfoList = new ArrayList<>();
        try {
            Element hotelWrapEle = document.select("div.hotelwrap").first();
            if (null == hotelWrapEle) return businessAreaInfoList;

            Element normalTitleEle = hotelWrapEle.select("div.normaltitle").first();
            if (null == normalTitleEle) return businessAreaInfoList;

            Element percentEle = normalTitleEle.select("span.percent").first();
            if (null == percentEle) return businessAreaInfoList;

            for (Element aEle : percentEle.select("a")) {
                Map<String, String> businessAreaMap = new HashMap<>();
                businessAreaMap.put("businessAreaLink", "http://you.ctrip.com" + aEle.attr("href").trim());
                String businessAreaDesc = aEle.text().trim();
                String patternStr = "(\\d+%)";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher m = pattern.matcher(businessAreaDesc);
                if (m.find()) {
                    businessAreaMap.put("businessAreaRatio", m.group(1).trim());
                    businessAreaMap.put("businessAreaName", businessAreaDesc.substring(0, businessAreaDesc.indexOf(m.group(1))));
                }
                businessAreaInfoList.add(businessAreaMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return businessAreaInfoList;
    }

    public TrspHttpRequestParam buildRequestParam() {
        TrspHttpRequestParam trspHttpRequestParam = new TrspHttpRequestParam();
        trspHttpRequestParam.setTimeout(10000);
        trspHttpRequestParam.setEncoding("utf-8");
        trspHttpRequestParam.setProxyType(TrspProxyType.TRAD_ADSL);
        trspHttpRequestParam.setHttpMethod(TrspHttpMethod.GET);
        return trspHttpRequestParam;
    }
}
