package com.hackx.travelnotes;

import com.google.gson.Gson;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TuNiuTravelNotesExtraction {

    public static void main(String[] args) {
        String targetUrl = "http://www.tuniu.com/trips/12461787";

        try {
            Document document = Jsoup.connect(targetUrl).get();
            /*System.out.println(document.html());*/
            Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);
            /*System.out.println(travelNotesInfoAttribute);*/
            List<List<Map<String, String>>> travelNotesContentSections = getTravelNotesContentSections(document);
            System.out.println(new Gson().toJson(travelNotesContentSections));
        } catch (Exception e) {
            System.out.println("Exception when connect to targetUrl:" + targetUrl + e.getMessage());
        }
    }

    public static Map<String, String> getTravelNotesInfoAttribute(Document document) {
        Map<String, String> travelNotesInfoAttribute = new HashMap<>();
        /* 文章标题 */
        Element detailTitleEle = document.select(".headtext").first();
        if (null != detailTitleEle) {
            travelNotesInfoAttribute.put("detailTitle", detailTitleEle.text());
        }
        /* 达人名称 */
        Element autherNameEle = document.select("div.auther-name").first();
        if (null != autherNameEle) {
            travelNotesInfoAttribute.put("autherName", autherNameEle.select("a").first().ownText());
        }
        /* 发表时间 */
        Element autherPublishEle = document.select("div.auther-publish").first();
        if (null != autherPublishEle && null != autherPublishEle.select(".time").first()) {
            Element publishTimeEle = autherPublishEle.select(".time").first();
            travelNotesInfoAttribute.put("publishTime", publishTimeEle.ownText().replace("发表于:", ""));
        }
        /* 达人头像 */
        Element autherImgEle = document.select("div.auther-img").first();
        if (null != autherImgEle && null != autherImgEle.select("img").first()) {
            Element imgEle = autherImgEle.select("img").first();
            travelNotesInfoAttribute.put("autherImg", imgEle.attr("src"));
        }
        return travelNotesInfoAttribute;
    }

    public static List<List<Map<String, String>>> getTravelNotesContentSections(Document document) {
        List<List<Map<String, String>>> travelNotesContentSections = new ArrayList<>();
        try {
            Element divContentLeft = document.select("div.content-left").first();
            if (!divContentLeft.child(0).className().contains("section-tit")) {
                travelNotesContentSections.add(extractDefaultParagraph(divContentLeft));
            }
            Elements divSectionTitElements = divContentLeft.children();
            for (Element divSectionTitEle : divSectionTitElements) {
                if (divSectionTitEle.className().contains("section-tit")) {
                    travelNotesContentSections.add(extractSpecialParagraph(divSectionTitEle));
                }
            }
        } catch (Exception e) {
            System.out.println("Exception when getTravelNotesContentSections," + e);
        }
        return travelNotesContentSections;
    }

    public static List<Map<String, String>> extractSpecialParagraph(Element divSectionTitEle) {
        List<Map<String, String>> specialParagraphList = new ArrayList<>();
        /* 标题解析 */
        specialParagraphList.add(generateParagraphTitle(divSectionTitEle));
        /* 内容解析 */
        Element currSiblingElement = divSectionTitEle.nextElementSibling();
        while (null != currSiblingElement && !currSiblingElement.className().contains("section-tit")
                && !currSiblingElement.className().contains("count-num")
                && !currSiblingElement.className().contains("content-comment")) {
            Map<String, String> blockMap = new HashMap<>();
            if (currSiblingElement.className().contains("section-des") && !StringUtil.isBlank(currSiblingElement.text())) { /* 文字 */
                blockMap.put("content", currSiblingElement.text());
                blockMap.put("type", "TEXT");
                specialParagraphList.add(blockMap);
            } else if (currSiblingElement.className().contains("section-img")) { /* 图片 */
                Elements imgElements = currSiblingElement.getElementsByTag("img");
                if (null != imgElements && imgElements.size() >= 1) {
                    blockMap.put("content", imgElements.first().attr("data-src"));
                    blockMap.put("type", "PICTURE");

                }
                /* 图片POI */
                Elements sectionPoiElements = currSiblingElement.getElementsByClass("section-img-poi");
                if (null != sectionPoiElements && sectionPoiElements.size() >= 1) {
                    blockMap.put("POI", sectionPoiElements.first().text());
                }
                specialParagraphList.add(blockMap);
            }
            currSiblingElement = currSiblingElement.nextElementSibling();
        }
        return specialParagraphList;
    }

    public static List<Map<String, String>> extractDefaultParagraph(Element divContentLeft) {
        List<Map<String, String>> defaultParagraphList = new ArrayList<>();
        defaultParagraphList.add(generateParagraphTitle(null));
        Elements sectionsElements = divContentLeft.children();
        for (Element sectionEle : sectionsElements) {
            if (sectionEle.className().contains("section-tit")
                    || sectionEle.className().contains("count-num")
                    || sectionEle.className().contains("content-comment")) {
                break;
            } else if (sectionEle.className().contains("section-des") && !StringUtil.isBlank(sectionEle.text())) { /* 文字 */
                Map<String, String> blockMap = new HashMap<>();
                blockMap.put("content", sectionEle.text());
                blockMap.put("type", "TEXT");
                defaultParagraphList.add(blockMap);
            } else if (sectionEle.className().contains("section-img")) { /* 图片 */
                Elements imgElements = sectionEle.getElementsByTag("img");
                if (null != imgElements && imgElements.size() >= 1) {
                    Map<String, String> blockMap = new HashMap<>();
                    blockMap.put("content", imgElements.first().attr("data-src"));
                    blockMap.put("type", "PICTURE");
                    defaultParagraphList.add(blockMap);
                }
            }
        }
        return defaultParagraphList;
    }

    public static Map<String, String> generateParagraphTitle(Element element) {
        Map<String, String> titleBlock = new HashMap<>();
        if (null == element) {
            titleBlock.put("content", "默认段落");
        } else {
            titleBlock.put("content", element.text());
        }
        titleBlock.put("type", "TITLE");
        return titleBlock;
    }
}
