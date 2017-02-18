package com.hackx.spiders;

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

public class QyerNotesDetailCrawler extends TrspCrawlerExtractorAdapter {
    @Override
    protected JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
        Document document = TrspExtractUtils.toDocument(html);
        JSONArray noteDetail = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", extractNoteDetail(document));
        noteDetail.add(jsonObject);
        return noteDetail;
    }

    public static List<Map<String, String>> extractNoteDetail(Document document) {
        List<Map<String, String>> noteDetailList = new ArrayList<>();
        try {
            Map<String, String> noteAttributeMap = new HashMap<>();
            noteAttributeMap = getTravelNoteAttribute(document);
            noteDetailList.add(noteAttributeMap);
            noteDetailList.addAll(getTravelNoteContent(document, noteAttributeMap.get("authorId")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noteDetailList;
    }

    public static Map<String, String> getTravelNoteAttribute(Document document) {
        Map<String, String> noteAttributeMap = new HashMap<>();
        try {
            /* 游记标题 */
            Elements titleElements = document.select("h3.title");
            if (null != titleElements && titleElements.size() > 0) {
                noteAttributeMap.put("noteTitle", titleElements.get(0).text());
            }

            /* 作者头像链接 */
            Element userphotoEle = document.select("a[data-bn-ipg=bbs-thread-top-userphoto]").first();
            if (null != userphotoEle && userphotoEle.select("img").size() > 0) {
                noteAttributeMap.put("authorPic", userphotoEle.select("img").first().attr("src"));
            }

            /* 作者名称 */
            Element userNameEle = document.select("a[data-bn-ipg=bbs-thread-top-username]").first();
            if (null != userNameEle) {
                noteAttributeMap.put("authorName", userNameEle.text());
                if (!StringUtil.isBlank(userNameEle.attr("href"))) {
                    String[] hrefFields = userNameEle.attr("href").split("/");
                    noteAttributeMap.put("authorId", hrefFields[hrefFields.length - 1]);
                }
            }

            /* 发布时间 */
            Element publishTimeEle = document.select("span.datetxt").first();
            if (null != publishTimeEle) {
                noteAttributeMap.put("publishTime", publishTimeEle.text());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noteAttributeMap;
    }

    public static List<Map<String, String>> getTravelNoteContent(Document document, Object authorId) {
        List<Map<String, String>> contentSectionsList = new ArrayList<>();
        Elements bbsDetailItemElements = document.select("div.bbs_detail_item");

        try {
            for (Element bbsDetailItemEle : bbsDetailItemElements) {
                if (authorId == null) {
                    throw new Exception("作者ID为NULL");
                } else if (bbsDetailItemEle.attr("data-uid").equals(authorId.toString())) {
                    contentSectionsList.addAll(extractSection(bbsDetailItemEle));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contentSectionsList;
    }

    public static List<Map<String, String>> extractSection(Element bbsDetailItemEle) {
        List<Map<String, String>> sectionBlocksList = new ArrayList<>();

        Element bbsDetailContainerEle = bbsDetailItemEle.select("td.bbsDetailContainer").first();
        if (null == bbsDetailContainerEle || bbsDetailContainerEle.children().size() == 0) {
            return sectionBlocksList;
        }
        for (Element childEle : bbsDetailContainerEle.children()) {
            Elements imgElements = childEle.select("img[data-type=photo]");
            if (null != imgElements && imgElements.size() > 0) {
                for (Element imgEle : imgElements) {
                    Map<String, String> pictureMap = new HashMap<>();
                    pictureMap.put("content", "http:" + imgEle.attr("src"));
                    pictureMap.put("type", "PICTURE");
                    if (null != imgEle.nextElementSibling() && imgEle.nextElementSibling().hasClass("imagedest-wrap")) {
                        pictureMap.put("poi", imgEle.nextElementSibling().text().trim());
                    }
                    sectionBlocksList.add(pictureMap);
                }
            } else {
                if ("h1".equals(childEle.tagName())) {
                    sectionBlocksList.add(generateBlockMap(childEle.text(), "FIRST_LEVEL_TITLE"));
                } else if ("h2".equals(childEle.tagName())) {
                    sectionBlocksList.add(generateBlockMap(childEle.text(), "SECOND_LEVEL_TITLE"));
                } else {
                    if (!StringUtil.isBlank(childEle.text().trim())) {
                        sectionBlocksList.add(generateBlockMap(childEle.text().trim(), "TEXT"));
                    }
                }
            }
        }
        return sectionBlocksList;
    }

    public static Map<String, String> generateBlockMap(String content, String type) {
        Map<String, String> map = new HashMap<>();
        map.put("content", content);
        map.put("type", type);
        return map;
    }

    public static void main(String[] args) {
        String targetUrl = "http://bbs.qyer.com/thread-2668276-1.html";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(extractNoteDetail(document));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateUrls() {
        String urls = "http://bbs.qyer.com/thread-2668276-1.html\n" +
                "http://bbs.qyer.com/thread-2659317-1.html\n" +
                "http://bbs.qyer.com/thread-2641539-1.html\n" +
                "http://bbs.qyer.com/thread-2561399-1.html\n" +
                "http://bbs.qyer.com/thread-2530730-1.html\n" +
                "http://bbs.qyer.com/thread-2488277-1.html\n" +
                "http://bbs.qyer.com/thread-1382116-1.html\n" +
                "http://bbs.qyer.com/thread-1111967-1.html\n" +
                "http://bbs.qyer.com/thread-1045602-1.html\n" +
                "http://bbs.qyer.com/thread-956894-1.html\n" +
                "http://bbs.qyer.com/thread-2483752-1.html\n" +
                "http://bbs.qyer.com/thread-1385568-1.html\n" +
                "http://bbs.qyer.com/thread-959850-1.html\n" +
                "http://bbs.qyer.com/thread-1039123-1.html\n" +
                "http://bbs.qyer.com/thread-1078067-1.html\n" +
                "http://bbs.qyer.com/thread-1422201-1.html\n" +
                "http://bbs.qyer.com/thread-1452168-1.html\n" +
                "http://bbs.qyer.com/thread-2504926-1.html\n" +
                "http://bbs.qyer.com/thread-2617823-1.html\n" +
                "http://bbs.qyer.com/thread-600996-1.html\n" +
                "http://bbs.qyer.com/thread-797799-1.html\n" +
                "http://bbs.qyer.com/thread-821198-1.html\n" +
                "http://bbs.qyer.com/thread-821253-1.html\n" +
                "http://bbs.qyer.com/thread-821314-1.html\n" +
                "http://bbs.qyer.com/thread-822190-1.html\n" +
                "http://bbs.qyer.com/thread-852400-1.html\n" +
                "http://bbs.qyer.com/thread-854143-1.html\n" +
                "http://bbs.qyer.com/thread-862179-1.html\n" +
                "http://bbs.qyer.com/thread-903889-1.html\n" +
                "http://bbs.qyer.com/thread-906669-1.html\n" +
                "http://bbs.qyer.com/thread-927645-1.html\n" +
                "http://bbs.qyer.com/thread-960430-1.html\n" +
                "http://bbs.qyer.com/thread-1016674-1.html\n" +
                "http://bbs.qyer.com/thread-1037392-1.html\n" +
                "http://bbs.qyer.com/thread-1064869-1.html\n" +
                "http://bbs.qyer.com/thread-1344610-1.html\n" +
                "http://bbs.qyer.com/thread-1392730-1.html\n" +
                "http://bbs.qyer.com/thread-1419437-1.html\n" +
                "http://bbs.qyer.com/thread-1465473-1.html\n" +
                "http://bbs.qyer.com/thread-2487763-1.html\n" +
                "http://bbs.qyer.com/thread-2509615-1.html\n" +
                "http://bbs.qyer.com/thread-2586182-1.html\n" +
                "http://bbs.qyer.com/thread-2613282-1.html\n" +
                "http://bbs.qyer.com/thread-2622243-1.html\n" +
                "http://bbs.qyer.com/thread-2630040-1.html\n" +
                "http://bbs.qyer.com/thread-2669192-1.html\n" +
                "http://bbs.qyer.com/thread-1035914-1.html\n" +
                "http://bbs.qyer.com/thread-1419416-1.html\n" +
                "http://bbs.qyer.com/thread-1447329-1.html\n" +
                "http://bbs.qyer.com/thread-1457823-1.html\n" +
                "http://bbs.qyer.com/thread-1458177-1.html\n" +
                "http://bbs.qyer.com/thread-2484661-1.html\n" +
                "http://bbs.qyer.com/thread-2499218-1.html\n" +
                "http://bbs.qyer.com/thread-2505770-1.html\n" +
                "http://bbs.qyer.com/thread-2519407-1.html\n" +
                "http://bbs.qyer.com/thread-2540380-1.html\n" +
                "http://bbs.qyer.com/thread-2553787-1.html\n" +
                "http://bbs.qyer.com/thread-2588419-1.html\n" +
                "http://bbs.qyer.com/thread-2618275-1.html\n" +
                "http://bbs.qyer.com/thread-2630472-1.html\n" +
                "http://bbs.qyer.com/thread-348043-1.html\n" +
                "http://bbs.qyer.com/thread-312450-1.html\n" +
                "http://bbs.qyer.com/thread-329187-1.html\n" +
                "http://bbs.qyer.com/thread-326554-1.html\n" +
                "http://bbs.qyer.com/thread-312626-1.html\n" +
                "http://bbs.qyer.com/thread-312307-1.html";
        for (String url : urls.split("\n")) {
            System.out.println("url:'" + url + "'");
        }
    }

}