package com.hackx.notescrawler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TongChengNotesDetailCrawler extends TrspCrawlerExtractorAdapter {

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

    public static Map<String, String> getTravelNoteAttribute(Document document) {
        Map<String, String> travelNoteAttribute = new HashMap<>();
        travelNoteAttribute.put("type", "NOTE_ATTRIBUTE");
        travelNoteAttribute.put("link", document.baseUri());
        try {
            Element tarvelDirList = document.select("div.mainLeftTop").first();
            if (null != tarvelDirList) {
                Element dayEle = tarvelDirList.select("span.mainLeft-day").first();
                if (null != dayEle) {
                    travelNoteAttribute.put("dayCount", dayEle.text().replace("行程天数：", ""));
                }
                Element rmbEle = tarvelDirList.select("span.mainLeft-rmb").first();
                if (null != rmbEle) {
                    travelNoteAttribute.put("avgFee", rmbEle.text().replace("人均消费：", ""));
                }
                Element peopleEle = tarvelDirList.select("span.mainLeft-people").first();
                if (null != peopleEle) {
                    travelNoteAttribute.put("people", peopleEle.text().replace("和谁一起：", ""));
                }
                Element wayEle = tarvelDirList.select("span.mainLeft-way").first();
                if (null != peopleEle) {
                    travelNoteAttribute.put("travelWay", wayEle.text().replace("旅行方式：", ""));
                }
            }
            Element divTitlemidEle = document.select("div.titlemid").first();
            if (null != divTitlemidEle) {
                travelNoteAttribute.put("noteTitle", divTitlemidEle.text());
            }
            Element createTimeEle = document.select("span.createTime").first();
            if (null != createTimeEle) {
                travelNoteAttribute.put("publishTime", createTimeEle.text().replace("发布于：", ""));
            }
            Element headnameEle = document.select("div.headname").first();
            if (null != headnameEle) {
                travelNoteAttribute.put("authorName", headnameEle.text());
            }
            Element headimgboxEle = document.select("div.headimgbox").first();
            if (null != headimgboxEle) {
                travelNoteAttribute.put("authorPic", "http" + headimgboxEle.select("img").first().attr("src"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return travelNoteAttribute;
    }

    public static Map<String, String> getFirstLevelTitle(Element dayHeadBg) {
        Map<String, String> firstLevelTitleMap = new HashMap<>();
        /* 一级标题 */
        StringBuffer firstLevelTitle = new StringBuffer();
        Elements divElements = dayHeadBg.children().select("div");
        for (Element divEle : divElements) {
            firstLevelTitle.append(divEle.text());
            firstLevelTitle.append(" ");
        }
        firstLevelTitleMap.put("content", firstLevelTitle.toString().trim());
        firstLevelTitleMap.put("type", "FIRST_LEVEL_TITLE");
        return firstLevelTitleMap;
    }

    public static List<Map<String, String>> getTravelNoteContent(Document document) {
        List<Map<String, String>> noteContentList = new ArrayList<>();
        Elements dayHeadBgElements = document.select("div.dayHeadBg");
        for (Element dayHeadBg : dayHeadBgElements) {
            noteContentList.add(getFirstLevelTitle(dayHeadBg));
            List<Map<String, String>> contentSection = extractOneDaySection(dayHeadBg);
            noteContentList.addAll(contentSection);
        }
        return noteContentList;
    }

    public static List<Map<String, String>> extractOneDaySection(Element dayHeadBg) {
        List<Map<String, String>> contextBlocksInDay = new ArrayList<>();
        Elements siblingElements = dayHeadBg.siblingElements();
        for (Element siblingElement : siblingElements) {
            if (siblingElement.className().contains("mainDayTitle")) {
                contextBlocksInDay.addAll(extractBlocks(siblingElement));
            }
        }
        return contextBlocksInDay;
    }

    public static List<Map<String, String>> extractBlocks(Element mainDayTitleElement) {
        List<Map<String, String>> contentBlocks = new ArrayList<>();
        /* 标题处理 */
        Map<String, String> titleBlock = new HashMap<>();
        titleBlock.put("content", mainDayTitleElement.text());
        titleBlock.put("type", "SECOND_LEVEL_TITLE");
        contentBlocks.add(titleBlock);

        /* 内容处理 */
        Element contentElement = mainDayTitleElement.nextElementSibling();
        for (Element childEle : contentElement.children()) {
            if (childEle.tagName().equals("p") && childEle.id().startsWith("txtCon")) {
                Map<String, String> contentBlock = new HashMap<>();
                Document noteContentDoc = Jsoup.parse(childEle.html().replaceAll(" ", "\\\\n"));
                contentBlock.put("content", noteContentDoc.text());
                contentBlock.put("type", "TEXT");
                contentBlocks.add(contentBlock);
            } else if (childEle.tagName().equals("div") && childEle.id().startsWith("txtCon")
                    && childEle.className().contains("imgwith")) {
                Elements imgElements = childEle.getElementsByTag("img");
                if (null != imgElements && imgElements.size() >= 1) {
                    Map<String, String> contentBlock = new HashMap<>();
                    contentBlock.put("content", imgElements.first().attr("data-img-src").substring(2));
                    contentBlock.put("type", "PICTURE");
                    contentBlocks.add(contentBlock);
                }
            }
        }
        return contentBlocks;
    }

}
