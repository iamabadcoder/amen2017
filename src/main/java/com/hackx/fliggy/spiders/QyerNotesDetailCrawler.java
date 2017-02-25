package com.hackx.fliggy.spiders;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerExtractorAdapter;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QyerNotesDetailCrawler extends TrspCrawlerExtractorAdapter {
    public static List<Map<String, String>> extractNoteDetail(Document document, JSONObject param) {
        List<Map<String, String>> noteDetailList = new ArrayList<>();
        try {
            int pageIndex = Integer.parseInt(String.valueOf(param.get("_pageIndex")));
            if (pageIndex == 0) {
                noteDetailList.add(getTravelNoteAttribute(document));
            }
            String pageNum = getPageNumFormLocation(document.location());
            if (null != pageNum && Integer.parseInt(pageNum) == (pageIndex + 1)){
                List<Map<String, String>> noteContent = getTravelNoteContent(document, getAuthorId(document));
                noteDetailList.addAll(noteContent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noteDetailList;
    }

    public static String getPageNumFormLocation(String localtion) {
        String[] fields = localtion.split("-");
        if (fields.length == 3) {
            return fields[2].trim().replace(".html", "");
        }
        return null;
    }

    public static String getAuthorId(Document document) {
        Element userNameEle = document.select("a[data-bn-ipg=bbs-thread-top-username]").first();
        if (null != userNameEle) {
            if (!StringUtil.isBlank(userNameEle.attr("href"))) {
                String[] hrefFields = userNameEle.attr("href").split("/");
                return hrefFields[hrefFields.length - 1];
            }
        }
        return null;
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
                    if (null == bbsDetailItemEle.select("div.bbsDetailContentQuote").first()) {
                        contentSectionsList.addAll(extractSection(bbsDetailItemEle));
                    }
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
        for (Element firstLevelChild : bbsDetailContainerEle.children()) {
            Elements photoElements = firstLevelChild.select("img[data-type=photo]");
            if (photoElements.size() == 0 && StringUtil.isBlank(firstLevelChild.text())) { /* 无图片,无文本*/
                continue;
            } else if (photoElements.size() == 0 && !StringUtil.isBlank(firstLevelChild.text())) { /* 无图片,有文本*/
                if ("h1".equals(firstLevelChild.tagName())) {
                    sectionBlocksList.add(generateBlockMap(firstLevelChild.text(), "FIRST_LEVEL_TITLE"));
                } else if ("h2".equals(firstLevelChild.tagName())) {
                    sectionBlocksList.add(generateBlockMap(firstLevelChild.text(), "SECOND_LEVEL_TITLE"));
                } else {
                    sectionBlocksList.add(generateBlockMap(firstLevelChild.text().trim(), "TEXT"));
                }
            } else if (photoElements.size() > 0 && StringUtil.isBlank(firstLevelChild.text())) { /* 有图片,无文本*/
                for (Element imgEle : photoElements) {
                    Map<String, String> pictureMap = new HashMap<>();
                    pictureMap.put("content", "http:" + imgEle.attr("data-original"));
                    pictureMap.put("type", "PICTURE");
                    /*if (null != imgEle.nextElementSibling() && imgEle.nextElementSibling().hasClass("imagedest-wrap")) {
                        pictureMap.put("poi", imgEle.nextElementSibling().text().trim());
                    }*/
                    sectionBlocksList.add(pictureMap);
                }
            } else if (photoElements.size() > 0 && !StringUtil.isBlank(firstLevelChild.text())) { /* 有图片,有文本*/
                sectionBlocksList.addAll(extractMixedElement(firstLevelChild));
            }
        }
        return sectionBlocksList;
    }

    public static List<Map<String, String>> extractMixedElement(Element firstLevelElement) {
        List<Map<String, String>> list = new ArrayList<>();
        List<Node> childNodes = firstLevelElement.childNodes();
        String textBlock = "";
        for (Node node : childNodes) {
            if ("#text".equals(node.nodeName())) {
                textBlock = textBlock + node.outerHtml();
                continue;
            }
            Element childEle = (Element) node;
            Element imgEle = childEle.select("img[data-type=photo]").first();
            if (null != imgEle) {
                if (!StringUtil.isBlank(textBlock)) {
                    list.add(generateBlockMap(textBlock, "TEXT"));
                    textBlock = "";
                }

                Map<String, String> pictureMap = new HashMap<>();
                pictureMap.put("content", "http:" + imgEle.attr("data-original"));
                pictureMap.put("type", "PICTURE");
                /*if (null != imgEle.nextElementSibling() && imgEle.nextElementSibling().hasClass("imagedest-wrap")) {
                    pictureMap.put("poi", imgEle.nextElementSibling().text().trim());
                }*/
                list.add(pictureMap);

                /*list.add(generateBlockMap("http:" + imgEle.attr("data-original"), "PICTURE"));
                if (StringUtil.isBlank(imgEle.attr("src"))) {
                    list.add(generateBlockMap("http:" + imgEle.attr("data-src"), "PICTURE"));
                } else {
                    list.add(generateBlockMap(imgEle.attr("src"), "PICTURE"));
                }*/
            } else {
                textBlock = textBlock + childEle.text();
            }
        }
        if (!StringUtil.isBlank(textBlock)) {
            list.add(generateBlockMap(textBlock, "TEXT"));
        }
        return list;
    }

    public static Map<String, String> generateBlockMap(String content, String type) {
        Map<String, String> map = new HashMap<>();
        map.put("content", content);
        map.put("type", type);
        return map;
    }

    public static void main(String[] args) {
        String targetUrl = "http://bbs.qyer.com/thread-2659317-2.html";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(extractNoteDetail(document, new JSONObject()));
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
        String regEx = "-([0-9]+)-";
        Pattern pattern = Pattern.compile(regEx);
        for (String url : urls.split("\n")) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                System.out.println("id:'" + matcher.group(1) + "'");
            }
        }
    }

    @Override
    protected JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
        Document document = TrspExtractUtils.toDocument(html);
        JSONArray noteDetail = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        List<Map<String, String>> detailList = extractNoteDetail(document, param);
        if (detailList.size() > 0) {
            jsonObject.put("data", detailList);
            noteDetail.add(jsonObject);
        }
        return noteDetail;
    }

}
