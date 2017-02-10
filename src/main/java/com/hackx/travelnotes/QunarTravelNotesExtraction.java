package com.hackx.travelnotes;

import com.alibaba.fastjson.JSON;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hackx on 2/9/17.
 */
public class QunarTravelNotesExtraction {

    public static void main(String[] args) {
        String targetUrl = "http://travel.qunar.com/youji/6727952";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            /*System.out.println(document.html());*/
            Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);
            /*System.out.println(travelNotesInfoAttribute);*/
            List<Map<String, List<Map<String, String>>>> travelNotesContentSections = getTravelNotesContentSections(document);
            System.out.println(JSON.toJSONString(travelNotesContentSections) + "}]");
        } catch (Exception e) {
            System.out.println("Exception when connect to targetUrl:" + targetUrl + e);
        }
    }

    public static List<Map<String, List<Map<String, String>>>> getTravelNotesContentSections(Document document) {
        List<Map<String, List<Map<String, String>>>> contentSectionsList = new ArrayList<>();
        Elements eDayElements = document.select("div.e_day");
        for (Element eDayEle : eDayElements) {
            /* 一级标题 */
            Element periodHdEle = eDayEle.select(".period_hd").first();
            if (periodHdEle != null && !StringUtil.isBlank(periodHdEle.text())) {
                Map<String, List<Map<String, String>>> map = new HashMap<>();
                List<Map<String, String>> contentSection = extractOneDaySection(eDayEle);
                map.put(periodHdEle.text(), contentSection);
                contentSectionsList.add(map);
            }
        }
        return contentSectionsList;
    }

    public static List<Map<String, String>> extractOneDaySection(Element eDayEle) {
        List<Map<String, String>> contextBlocksInDay = new ArrayList<>();
        Element periodCtEle = eDayEle.select("div.period_ct").first();
        Elements bPoiInfoElements = periodCtEle.select("div.b_poi_info");
        for (Element bPoiInfoEle : bPoiInfoElements) {
            Element bPoiTitleBoxEle = bPoiInfoEle.select("div.b_poi_title_box").first();
            if (null != bPoiTitleBoxEle) {
                contextBlocksInDay.add(generateBlockMap(bPoiTitleBoxEle.text(), "TITLE"));
            }

            Element textEle = bPoiInfoEle.select("div.text").first();
            if (null != textEle) {
                contextBlocksInDay.add(generateBlockMap(textEle.text(), "TEXT"));
            }


            Element bottomEle = bPoiInfoEle.select("div.bottom").first();
            if (null != bottomEle) {
                Element imglstEle = bottomEle.select("div.imglst").first();
                if (null != imglstEle) {
                    Elements dlElements = imglstEle.select("dl");
                    for (Element dlEle : dlElements) {
                        Element dtEle = dlEle.select("dt").first();
                        Element imgEle = dtEle.select("img.box_img").first();
                        contextBlocksInDay.add(generateBlockMap(imgEle.attr("data-original"), "PICTURE"));

                        Element ddEle = dlEle.select("dd").first();
                        if (null != ddEle) {
                            contextBlocksInDay.add(generateBlockMap(ddEle.text(), "TEXT"));
                        }
                    }
                }
            }
        }
        return contextBlocksInDay;
    }

    public static Map<String, String> generateBlockMap(String content, String type) {
        return new HashMap<String, String>() {
            {
                put("content", content);
                put("type", type);
            }
        };
    }

    public static Map<String, String> getTravelNotesInfoAttribute(Document document) {
        Map<String, String> travelNotesInfoAttribute = new HashMap<>();
        /* 作者头像链接 */
        Element userHeadsEle = document.select("img.user_heads").first();
        if (null != userHeadsEle) {
            travelNotesInfoAttribute.put("userPhoto", userHeadsEle.attr("src"));
        }
        /* 作者名称 */
        Element userNameEle = document.select("li.head").first();
        if (null != userNameEle) {
            travelNotesInfoAttribute.put("userName", userNameEle.text());
        }
        /* 发布时间 */
        Element publishTimeEle = document.select("li.date").first();
        if (null != publishTimeEle) {
            travelNotesInfoAttribute.put("publishTime", publishTimeEle.child(0).text());
        }
        /* 游记标题 */
        Element titleEle = document.select("div.fix-title").first();
        if (null != titleEle) {
            travelNotesInfoAttribute.put("title", titleEle.text());
        }
        Element forewordListEle = document.select(".foreword_list").first();
        if (null != forewordListEle) {
            /* 出发日期 */
            Element whenEle = forewordListEle.select(".when").first();
            if (null != whenEle) {
                travelNotesInfoAttribute.put("startTime", whenEle.text().replaceAll("出发日期/?", ""));
            }
            /* 天数 */
            Element howLongEle = forewordListEle.select(".howlong").first();
            if (null != howLongEle) {
                travelNotesInfoAttribute.put("dayCount", howLongEle.text().replaceAll("天数/?", ""));
            }
            /* 人物 */
            Element whoEle = forewordListEle.select(".who").first();
            if (null != whoEle) {
                travelNotesInfoAttribute.put("people", whoEle.text().replaceAll("人物/?", ""));
            }
            /* 玩法 */
            Element howEle = forewordListEle.select(".how").first();
            if (null != howEle) {
                travelNotesInfoAttribute.put("howInfo", howEle.text().replaceAll("玩法/?", ""));
            }
        }
        return travelNotesInfoAttribute;
    }

}
