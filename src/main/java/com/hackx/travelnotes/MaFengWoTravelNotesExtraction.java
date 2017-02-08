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

public class MaFengWoTravelNotesExtraction {

    public static void main(String[] args) {

        String targetUrl = "http://www.mafengwo.cn/i/5336490.html";

        Gson gson = new Gson();
        try {
            Document document = Jsoup.connect(targetUrl).get();
            /*System.out.println(document.html());*/
            Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);
            /*System.out.println(travelNotesInfoAttribute);*/
            List<List<Map<String, String>>> travelNotesContentSections = getTravelNotesContentSections(document);
            System.out.println(gson.toJson(travelNotesContentSections));
        } catch (Exception e) {
            System.out.println("Exception when connect to targetUrl:" + targetUrl + e.getMessage());
        }
    }

    public static List<List<Map<String, String>>> getTravelNotesContentSections(Document document) {
        List<List<Map<String, String>>> contentSectionsList = new ArrayList<>();
        Element jMasterContent = document.select("div._j_master_content").first();
        /* 默认段落 */
        List<Map<String, String>> contentSection = new ArrayList<>();
        if (!jMasterContent.child(0).className().contains("article_title")) {
            contentSection = extractDefaultParagraph(jMasterContent);
            contentSectionsList.add(contentSection);
        }
        Elements articleTitleElements = jMasterContent.getElementsByClass("article_title");
        for (Element articleTitle : articleTitleElements) {
            contentSection = extractParagraph(articleTitle);
            contentSectionsList.add(contentSection);
        }
        return contentSectionsList;
    }

    public static List<Map<String, String>> extractParagraph(Element articleTitle) {
        List<Map<String, String>> contentBlocks = new ArrayList<>();

        /* 标题处理 */
        Map<String, String> titleBlock = new HashMap<>();
        titleBlock.put("content", articleTitle.text());
        titleBlock.put("type", "TITLE");
        contentBlocks.add(titleBlock);
        if (StringUtil.isBlank(articleTitle.text())) {
            if (articleTitle.select("img").first() != null) {
                titleBlock.put("content", articleTitle.select("img").first().attr("title"));
            }
        }

        /* 内容处理 */
        Element currElement = articleTitle.nextElementSibling();
        for (int i = 0; i < 100; i++) {
            if (null == currElement) {
                break;
            }
            Element nextElementSibling = currElement;
            Map<String, String> contentBlock = new HashMap<>();
            if (nextElementSibling.className().contains("article_title")) { /* 标题 */
                break;
            } else if (nextElementSibling.className().contains("_j_note_content _j_seqitem")) { /* 文本 */
                Document noteContentDoc = Jsoup.parse(nextElementSibling.html().replaceAll("<br>", "\\n"));
                contentBlock.put("content", noteContentDoc.text());
                contentBlock.put("type", "TEXT");
                contentBlocks.add(contentBlock);
            } else if (nextElementSibling.className().contains("add_pic _j_anchorcnt _j_seqitem")) { /* 图片 */
                /* 图片地址 */
                Elements imgElements = nextElementSibling.getElementsByTag("img");
                if (null != imgElements && imgElements.size() >= 1) {
                    contentBlock.put("content", imgElements.first().attr("data-src"));
                    contentBlock.put("type", "PICTURE");
                    contentBlocks.add(contentBlock);
                }
                /* 图片POI */
                Elements imgPoiElements = nextElementSibling.select("a[href*=/poi/]");
                if (null != imgPoiElements && imgPoiElements.size() >= 1) {
                    contentBlock.put("POI", imgPoiElements.first().text());
                }

            } else if (nextElementSibling.className().contains("add_video _j_seqitem ")) { /* 视频 */
                contentBlock.put("content", nextElementSibling.attr("data-url"));
                contentBlock.put("type", "VIDEO");
                contentBlocks.add(contentBlock);
            }
            currElement = currElement.nextElementSibling();
        }
        return contentBlocks;
    }

    public static List<Map<String, String>> extractDefaultParagraph(Element jMasterContent) {
        List<Map<String, String>> contentBlocks = new ArrayList<>();

        /* 标题处理 */
        Map<String, String> titleBlock = new HashMap<>();
        titleBlock.put("content", "默认段落");
        titleBlock.put("type", "TITLE");
        contentBlocks.add(titleBlock);

        /* 内容处理 */
        for (int i = 0; i < 20; i++) {
            Element ithChild = jMasterContent.child(i);
            Map<String, String> contentBlock = new HashMap<>();
            if (ithChild.className().contains("article_title")) { /* 标题 */
                break;
            } else if (ithChild.className().contains("_j_note_content _j_seqitem")) { /* 文本 */
                Document noteContentDoc = Jsoup.parse(ithChild.html().replaceAll("<br>", "\\n"));

                contentBlock.put("content", noteContentDoc.text());
                contentBlock.put("type", "TEXT");
                contentBlocks.add(contentBlock);
            } else if (ithChild.className().contains("add_pic _j_anchorcnt _j_seqitem")) { /* 图片 */
                Elements imgElements = ithChild.getElementsByTag("img");
                if (null != imgElements && imgElements.size() >= 1) {
                    contentBlock.put("content", imgElements.first().attr("data-src"));
                    contentBlock.put("type", "PICTURE");
                    contentBlocks.add(contentBlock);
                }
            } else if (ithChild.className().contains("add_video _j_seqitem ")) { /* 视频 */
                contentBlock.put("content", ithChild.attr("data-url"));
                contentBlock.put("type", "VIDEO");
                contentBlocks.add(contentBlock);
            }
        }
        return contentBlocks;
    }

    public static Map<String, String> getTravelNotesInfoAttribute(Document document) {
        Map<String, String> travelNotesInfoAttribute = new HashMap<>();
        try {
            Element tarvelDirList = document.select("div.tarvel_dir_list").first();
            if (null != tarvelDirList) {
                Element timeEle = tarvelDirList.select("li.time").first();
                Element dayEle = tarvelDirList.select("li.day").first();
                Element peopleEle = tarvelDirList.select("li.people").first();
                Element costEle = tarvelDirList.select("li.cost").first();
                travelNotesInfoAttribute.put("beginTime", timeEle.ownText().replace("出发时间", ""));
                travelNotesInfoAttribute.put("travelDays", dayEle.ownText().replace("出行天数", ""));
                travelNotesInfoAttribute.put("people", peopleEle.ownText().replace("人物", ""));
                travelNotesInfoAttribute.put("avgFee", costEle.ownText().replace("人均费用", ""));
            }
        } catch (Exception e) {
            System.out.println("Exception when getTravelNotesInfoAttribute," + e);
        }
        return travelNotesInfoAttribute;
    }

}
