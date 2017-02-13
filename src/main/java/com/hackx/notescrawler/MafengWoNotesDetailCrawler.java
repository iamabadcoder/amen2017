package com.hackx.notescrawler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MafengWoNotesDetailCrawler extends TrspCrawlerExtractorAdapter {

    public JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
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

    public Map<String, String> getTravelNoteAttribute(Document document) {
        Map<String, String> travelNoteAttribute = new HashMap<>();
        travelNoteAttribute.put("type", "NOTE_ATTRIBUTE");
        travelNoteAttribute.put("link", document.baseUri());
        try {
            Element tarvelDirList = document.select("div.tarvel_dir_list").first();
            if (null != tarvelDirList) {
                Element timeEle = tarvelDirList.select("li.time").first();
                if (null != timeEle) {
                    travelNoteAttribute.put("beginTime", timeEle.ownText().replace("出发时间", ""));
                }
                Element dayEle = tarvelDirList.select("li.day").first();
                if (null != dayEle) {
                    travelNoteAttribute.put("dayCount", dayEle.ownText().replace("出行天数", ""));
                }
                Element peopleEle = tarvelDirList.select("li.people").first();
                if (null != peopleEle) {
                    travelNoteAttribute.put("people", peopleEle.ownText().replace("人物", ""));
                }
                Element costEle = tarvelDirList.select("li.cost").first();
                if (null != costEle) {
                    travelNoteAttribute.put("avgFee", costEle.ownText().replace("人均费用", ""));
                }
            }
            Element headTextEle = document.select("h1.headtext").first();
            if (null != headTextEle) {
                travelNoteAttribute.put("noteTitle", headTextEle.ownText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return travelNoteAttribute;
    }

    public List<Map<String, String>> getTravelNoteContent(Document document) {
        List<Map<String, String>> noteContentList = new ArrayList<>();
        Element jMasterContent = document.select("div._j_master_content").first();
        /* 默认段落 */
        if (!jMasterContent.child(0).className().contains("article_title")) {
            noteContentList.addAll(extractDefaultParagraph(jMasterContent));
        }
        Elements articleTitleElements = jMasterContent.getElementsByClass("article_title");
        for (Element articleTitle : articleTitleElements) {
            noteContentList.addAll(extractParagraph(articleTitle));
        }
        return noteContentList;
    }

    public List<Map<String, String>> extractParagraph(Element articleTitle) {
        List<Map<String, String>> contentBlocks = new ArrayList<>();

        /* 标题处理 */
        Map<String, String> titleBlock = new HashMap<>();
        titleBlock.put("content", articleTitle.text());
        titleBlock.put("type", "FIRST_LEVEL_TITLE");
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
                Document noteContentDoc = Jsoup.parse(nextElementSibling.html().replaceAll("<br>", "\\\\n"));
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
                Elements imgPoiElements = nextElementSibling.select("span.pic_tag");
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

    public List<Map<String, String>> extractDefaultParagraph(Element jMasterContent) {
        List<Map<String, String>> contentBlocks = new ArrayList<>();

        /* 标题处理 */
        Map<String, String> titleBlock = new HashMap<>();
        titleBlock.put("content", "默认段落");
        titleBlock.put("type", "FIRST_LEVEL_TITLE");
        contentBlocks.add(titleBlock);

        /* 内容处理 */
        for (int i = 0; i < 20; i++) {
            Element ithChild = jMasterContent.child(i);
            Map<String, String> contentBlock = new HashMap<>();
            if (ithChild.className().contains("article_title")) { /* 标题 */
                break;
            } else if (ithChild.className().contains("_j_note_content _j_seqitem")) { /* 文本 */
                Document noteContentDoc = Jsoup.parse(ithChild.html().replaceAll("<br>", "\\\\n"));

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

}
