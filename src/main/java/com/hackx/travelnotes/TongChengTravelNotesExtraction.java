package com.hackx.travelnotes;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TongChengTravelNotesExtraction {

    public static void main(String[] args) {
        String targetUrl = "http://go.ly.com/travels/159013.html";
        Gson gson = new Gson();
        try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(document.html());
            Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);
            /*System.out.println(travelNotesInfoAttribute);*/
            Map<String, List<List<Map<String, String>>>> contentSections = getTravelNotesContentSections(document);
            /*System.out.println(gson.toJson(contentSections));*/
        } catch (Exception e) {
            System.out.println("Exception when connect to targetUrl:" + targetUrl + e.getMessage());
        }

    }

    public static Map<String, List<List<Map<String, String>>>> getTravelNotesContentSections(Document document) {
        Map<String, List<List<Map<String, String>>>> contentSectionsMap = new HashMap<>();
        Elements dayHeadBgElements = document.select("div.dayHeadBg");
        for (Element dayHeadBg : dayHeadBgElements) {
            /* 一级标题 */
            StringBuffer firstLevelTitle = new StringBuffer();
            Elements divElements = dayHeadBg.select("div");
            for (Element divEle : divElements) {
                firstLevelTitle.append(divEle.text());
                firstLevelTitle.append(" ");
            }
            List<List<Map<String, String>>> contentSection = extractOneDaySection(dayHeadBg);
            contentSectionsMap.put(firstLevelTitle.toString().trim(), contentSection);
        }
        return contentSectionsMap;
    }

    public static List<List<Map<String, String>>> extractOneDaySection(Element dayHeadBg) {
        List<List<Map<String, String>>> contextBlocksInDay = new ArrayList<>();
        Elements siblingElements = dayHeadBg.siblingElements();
        for (Element siblingElement : siblingElements) {
            if (siblingElement.className().contains("mainDayTitle")) {
                List<Map<String, String>> contentBlocks = extractBlocks(siblingElement);
                contextBlocksInDay.add(contentBlocks);
            }
        }
        return contextBlocksInDay;
    }

    public static List<Map<String, String>> extractBlocks(Element mainDayTitleElement) {
        List<Map<String, String>> contentBlocks = new ArrayList<>();
        /* 标题处理 */
        Map<String, String> titleBlock = new HashMap<>();
        titleBlock.put("content", mainDayTitleElement.text());
        titleBlock.put("type", "TITLE");
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


    public static Map<String, String> getTravelNotesInfoAttribute(Document document) {
        Map<String, String> travelNotesInfoAttribute = new HashMap<>();
        try {
            Element tarvelDirList = document.select("div.mainLeftTop").first();
            if (null != tarvelDirList) {
                Element dayEle = tarvelDirList.select("span.mainLeft-day").first();
                Element rmbEle = tarvelDirList.select("span.mainLeft-rmb").first();
                Element peopleEle = tarvelDirList.select("span.mainLeft-people").first();
                Element wayEle = tarvelDirList.select("span.mainLeft-way").first();
                travelNotesInfoAttribute.put("dayCount", dayEle.text().replace("行程天数：", ""));
                travelNotesInfoAttribute.put("avgFee", rmbEle.text().replace("人均消费：", ""));
                travelNotesInfoAttribute.put("people", peopleEle.text().replace("和谁一起：", ""));
                travelNotesInfoAttribute.put("way", wayEle.text().replace("旅行方式：", ""));
            }
        } catch (Exception e) {
            System.out.println("Exception when getTravelNotesInfoAttribute," + e);
        }
        return travelNotesInfoAttribute;
    }


}
