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

public class HaoQiaoHotelBusinessAreaSpider extends TrspCrawlerAdapter {
    public static void main(String[] args) {
        generateParamters();
    }

    public static void generateParamters() {
        try {
            String targetUrl = "http://www.haoqiao.cn/sitemap.html";
            Document document = Jsoup.connect(targetUrl).get();
            Element divSiteMapEle = document.select("div.sitemap").first();
            if (null == divSiteMapEle) {
                return;
            }
            for (Element sitemapLinkEle : divSiteMapEle.select("div.sitemap-link")) {
                for (Element aEle : sitemapLinkEle.select("a")) {
                    if (aEle.attr("href").contains("all.html")) {
                        System.out.println("cityName:'" + aEle.text().trim() + "',cityLink:'" + aEle.attr("href").replace("all.html", "").trim() + "'");
                    } else {
                        System.out.println("cityName:'" + aEle.text().trim() + "',cityLink:'" + aEle.attr("href").trim() + "'");
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
        String cityName = crawlParam.getString("cityName");
        String cityLink = crawlParam.getString("cityLink");
        jsonObject.put("cityName", cityName);
        jsonObject.put("cityLink", cityLink);

        List<Map<String, String>> businessAreaList = new ArrayList<>();
        List<Map<String, List<String>>> hotelTitleList = new ArrayList<>();

        /* 商圈信息抽取 */
        TrspHttpRequestParam trspHttpRequestParam = buildRequestParam();
        trspHttpRequestParam.setUrl(cityLink);
        String cityDetailResponseBody = trspHttpManager.request(trspHttpRequestParam).getBody();
        businessAreaList = crawlBusinessAreas(Jsoup.parse(cityDetailResponseBody));
        jsonObject.put("businessAreaList", businessAreaList);

        /* 酒店列表抽取 */
        for (Map<String, String> map : businessAreaList) {
            String targetUrl = cityLink + "a" + map.get("businessAreaId").trim();
            trspHttpRequestParam.setUrl(targetUrl);
            String businessAreaResponseBody = trspHttpManager.request(trspHttpRequestParam).getBody();
            List<String> hotelTitles = crawlHotelInfo(Jsoup.parse(businessAreaResponseBody));
            Map<String, List<String>> hotelTitleMap = new HashMap<>();
            hotelTitleMap.put(map.get("businessAreaTitle"), hotelTitles);
            hotelTitleList.add(hotelTitleMap);
        }
        jsonObject.put("hotelTitleList", hotelTitleList);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        trspExtractResultDO.setResult(jsonArray);
        return trspExtractResultDO;
    }

    public TrspHttpRequestParam buildRequestParam() {
        TrspHttpRequestParam trspHttpRequestParam = new TrspHttpRequestParam();
        trspHttpRequestParam.setTimeout(10000);
        trspHttpRequestParam.setEncoding("utf-8");
        trspHttpRequestParam.setProxyType(TrspProxyType.TRAD_ADSL);
        trspHttpRequestParam.setHttpMethod(TrspHttpMethod.GET);
        return trspHttpRequestParam;
    }

    public List<String> crawlHotelInfo(Document document) {
        List<String> hotelInfoList = new ArrayList<>();
        for (Element hotelTitleEle : document.select("div.hotel-l-t")) {
            hotelInfoList.add(hotelTitleEle.ownText());
        }
        return hotelInfoList;
    }

    public List<Map<String, String>> crawlBusinessAreas(Document document) {
        List<Map<String, String>> businessAreaInfoList = new ArrayList<>();
        try {
            Element areaFilterEle = document.select("div#J_filter_area").first();
            if (areaFilterEle == null) return businessAreaInfoList;

            for (Element aEle : areaFilterEle.select("a")) {
                if (aEle.text().contains("全部区域")) {
                    continue;
                }
                String[] fields = aEle.attr("data-filter").split("\\|");
                if (fields.length == 2) {
                    Map<String, String> businessAreaMap = new HashMap<>();
                    businessAreaMap.put("businessAreaTitle", aEle.ownText().trim());
                    businessAreaMap.put("businessAreaId", fields[1].trim());
                    businessAreaInfoList.add(businessAreaMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            for (Element tipsEle : document.select("div.list-tips-t")) {
                String[] fields = tipsEle.ownText().trim().split("·");
                if (fields.length == 2) {
                    String businessAreaTitle = fields[1].trim();
                    String businessAreaDesc = tipsEle.nextElementSibling().select("dd").first().text();
                    for (Map<String, String> businessAreaMap : businessAreaInfoList) {
                        if (businessAreaMap.get("businessAreaTitle").toString().trim().equals(businessAreaTitle)) {
                            businessAreaMap.put("businessAreaDesc", businessAreaDesc);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return businessAreaInfoList;
    }

}
