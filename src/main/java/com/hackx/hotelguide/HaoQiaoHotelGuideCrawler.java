package com.hackx.hotelguide;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HaoQiaoHotelGuideCrawler {

    public static final String SEPARATOR_SIGN = "####";

    public static void main(String[] args) {
        Map<String, List<String>> siteMap = extractSiteMap("http://www.haoqiao.cn/sitemap.html");
        System.out.println(siteMap.values().stream().map((city) -> city.size()).reduce((a, b) -> a + b).get());
        for (Map.Entry<String, List<String>> entry : siteMap.entrySet()) {
            JSONObject jsonObject = new JSONObject();
            String[] countyrFields = entry.getKey().split(SEPARATOR_SIGN);
            if (countyrFields.length == 2) {
                jsonObject.put("contryName", countyrFields[0]);
                jsonObject.put("contryLink", countyrFields[1]);
            }
            for (String city : entry.getValue()) {
                try {
                    Thread.sleep(3000); /* 暂停3秒 */

                    String[] cityFields = city.split(SEPARATOR_SIGN);
                    System.out.println(cityFields[0]);
                    Document document = Jsoup.connect(cityFields[1].toString()).get();
                    if (document.select("div.zone-title-l").first() == null) {
                        continue;
                    }
                    jsonObject.put("CityName", cityFields[0]);
                    jsonObject.put("CityLink", cityFields[1]);

                    jsonObject.put("whereToLive", extractWhereToLiveInfo(document));
                    fillZoneInfo(jsonObject, document);
                    extractZoneBoundLatlngAndNames(jsonObject, document);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            write2File("HaoQiaoHotelGuideData.txt", jsonObject.toJSONString());
        }
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
