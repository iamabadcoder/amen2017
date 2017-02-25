package com.hackx.fliggy.spiders;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QunarNotesDetailCrawler extends TrspCrawlerExtractorAdapter {
    public static Map<String, String> getTravelNoteAttribute(Document document) {
        Map<String, String> noteAttributeMap = new HashMap<>();
        try {
            /* 作者头像链接 */
            Element userHeadsEle = document.select("img.user_heads").first();
            if (null != userHeadsEle) {
                noteAttributeMap.put("authorPic", userHeadsEle.attr("src"));
            }
            /* 作者名称 */
            Element userNameEle = document.select("li.head").first();
            if (null != userNameEle) {
                noteAttributeMap.put("authorName", userNameEle.text());
            }
            /* 发布时间 */
            Element publishTimeEle = document.select("li.date").first();
            if (null != publishTimeEle) {
                noteAttributeMap.put("publishTime", publishTimeEle.child(0).text());
            }
            /* 游记标题 */
            Element titleEle = document.select("div.fix-title").first();
            if (null != titleEle) {
                noteAttributeMap.put("noteTitle", titleEle.text());
            }
            Element forewordListEle = document.select(".foreword_list").first();
            if (null != forewordListEle) {
                /* 出发日期 */
                Element whenEle = forewordListEle.select(".when").first();
                if (null != whenEle) {
                    noteAttributeMap.put("beginTime", whenEle.text().replaceAll("出发日期/?", ""));
                }
                /* 天数 */
                Element howLongEle = forewordListEle.select(".howlong").first();
                if (null != howLongEle) {
                    noteAttributeMap.put("dayCount", howLongEle.text().replaceAll("天数/?", ""));
                }
                /* 人均消费 */
                Element howMuchEle = forewordListEle.select(".howmuch").first();
                if (null != howMuchEle) {
                    noteAttributeMap.put("avgFee", howMuchEle.text().replaceAll("人均费用/?", ""));
                }
                /* 人物 */
                Element whoEle = forewordListEle.select(".who").first();
                if (null != whoEle) {
                    noteAttributeMap.put("people", whoEle.text().replaceAll("人物/?", ""));
                }
                /* 玩法 */
                Element howEle = forewordListEle.select(".how").first();
                if (null != howEle) {
                    noteAttributeMap.put("howInfo", howEle.text().replaceAll("玩法/?", ""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noteAttributeMap;
    }

    public static List<Map<String, String>> getTravelNoteContent(Document document) {
        List<Map<String, String>> contentSectionsList = new ArrayList<>();
        Elements eDayElements = document.select("div.e_day");
        for (Element eDayEle : eDayElements) {
            Map<String, String> firstLevelTitleMap = getFirstLevelTitle(eDayEle);
            if (firstLevelTitleMap.size() > 0) {
                contentSectionsList.add(firstLevelTitleMap);
                contentSectionsList.addAll(extractOneDaySection(eDayEle));
            }
        }
        return contentSectionsList;
    }

    public static Map<String, String> getFirstLevelTitle(Element eDayEle) {
        Map<String, String> firstLevelTitleMap = new HashMap<>();
        Element periodHdEle = eDayEle.select(".period_hd").first();
        if (periodHdEle != null && !StringUtil.isBlank(periodHdEle.text())) {
            firstLevelTitleMap.put("content", periodHdEle.text());
            firstLevelTitleMap.put("type", "FIRST_LEVEL_TITLE");
        }
        return firstLevelTitleMap;
    }

    public static List<Map<String, String>> extractOneDaySection(Element eDayEle) {
        List<Map<String, String>> contextBlocksInDay = new ArrayList<>();
        Element periodCtEle = eDayEle.select("div.period_ct").first();
        Elements bPoiInfoElements = periodCtEle.select("div.b_poi_info");
        for (Element bPoiInfoEle : bPoiInfoElements) {
            Element bPoiTitleBoxEle = bPoiInfoEle.select("div.b_poi_title_box").first();
            if (null != bPoiTitleBoxEle) {
                contextBlocksInDay.add(generateBlockMap(bPoiTitleBoxEle.text(), "SECOND_LEVEL_TITLE"));
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
        Map<String, String> map = new HashMap<>();
        map.put("content", content);
        map.put("type", type);
        return map;
    }

    @Override
    protected JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
        Document document = TrspExtractUtils.toDocument(html);
        JSONArray noteDetail = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", extractNoteDetail(document));
        noteDetail.add(jsonObject);
        return noteDetail;
    }

    public List<Map<String, String>> extractNoteDetail(Document document) {
        List<Map<String, String>> noteDetailList = new ArrayList<>();
        try {
            noteDetailList.add(getTravelNoteAttribute(document));
            noteDetailList.addAll(getTravelNoteContent(document));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noteDetailList;
    }

}
