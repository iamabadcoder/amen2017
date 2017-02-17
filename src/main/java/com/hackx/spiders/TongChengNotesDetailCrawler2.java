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

import java.util.*;

public class TongChengNotesDetailCrawler2 extends TrspCrawlerExtractorAdapter {

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
            Element raidtitleEle = document.select("#raidtitle").first();
            if (null != raidtitleEle) {
                travelNoteAttribute.put("noteTitle", raidtitleEle.text());
            }
            Element usernamedivEle = document.select("div.usernamediv").first();
            if (null != usernamedivEle) {
                Element usernamespanEle = usernamedivEle.select("span.usernamespan").first();
                if (null != usernamespanEle) {
                    travelNoteAttribute.put("authorName", usernamespanEle.text());
                }
            }
            Element userimgbordEle = document.select("div.userimgbord").first();
            if (null != userimgbordEle) {
                Element userimgEle = userimgbordEle.select("img.userimg").first();
                if (null != userimgEle && userimgEle.attr("src").length() > 2) {
                    travelNoteAttribute.put("authorPic", userimgEle.attr("src").substring(2));
                }
            }
            Element leftcentEle = document.select("div.leftcent").first();
            if (null != leftcentEle) {

                List<String> noteTags = new ArrayList<>();
                Elements tagElements = leftcentEle.select("span.tagname");
                for (Element tagEle : tagElements) {
                    noteTags.add(tagEle.text());
                }
                travelNoteAttribute.put("noteTags", Arrays.toString(noteTags.toArray()));

                List<String> footprints = new ArrayList<>();
                Element mddulEle = leftcentEle.select("ul.mddul").first();
                if (null != mddulEle) {
                    for (Element aEle : mddulEle.select("a")) {
                        footprints.add(aEle.text());
                    }
                }
                travelNoteAttribute.put("footprints", Arrays.toString(footprints.toArray()));

                String leftcentText = leftcentEle.text();
                if (leftcentText.indexOf("游玩标签") > leftcentText.indexOf("出发城市")) {
                    travelNoteAttribute.put("departureCity", leftcentText.substring(leftcentText.indexOf("出发城市"), leftcentText.indexOf("游玩标签")).replace("出发城市", ""));
                }
                if (leftcentText.indexOf("游玩天数") > leftcentText.indexOf("出发时间")) {
                    travelNoteAttribute.put("beingTime", leftcentText.substring(leftcentText.indexOf("出发时间"), leftcentText.indexOf("游玩天数")).replace("出发时间", ""));
                }
                if (leftcentText.indexOf("游玩人数") > leftcentText.indexOf("游玩天数")) {
                    travelNoteAttribute.put("datCount", leftcentText.substring(leftcentText.indexOf("游玩天数"), leftcentText.indexOf("游玩人数")).replace("游玩天数", ""));
                }
                if (leftcentText.indexOf("游玩人数") > 0) {
                    travelNoteAttribute.put("people", leftcentText.substring(leftcentText.indexOf("游玩人数")).replace("游玩人数", ""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return travelNoteAttribute;
    }

    public static List<Map<String, String>> getTravelNoteContent(Document document) {
        List<Map<String, String>> noteContentList = new ArrayList<>();
        Elements zjmsgElements = document.select("div.zjmsg");
        for (Element zjmsgEle : zjmsgElements) {
            noteContentList.add(extractFirstLevelTitle(zjmsgEle));
            List<Map<String, String>> contentSection = extractSection(zjmsgEle);
            noteContentList.addAll(contentSection);
        }
        return noteContentList;
    }

    public static Map<String, String> extractFirstLevelTitle(Element zjmsgEle) {
        Map<String, String> map = new HashMap<>();
        map.put("type", "FIRST_LEVEL_TITLE");
        Element zjtitledivEle = zjmsgEle.select("div.zjtitlediv").first();
        if (null != zjtitledivEle) {
            map.put("content", zjtitledivEle.text().trim());
        }
        return map;
    }

    public static List<Map<String, String>> extractSection(Element zjmsgEle) {
        List<Map<String, String>> contextBlocks = new ArrayList<>();
        Element zjcontentEle = zjmsgEle.select("div.zjcontent").first();
        if (null == zjcontentEle) return contextBlocks;
        for (Element childEle : zjcontentEle.children()) {
            Elements imgElements = childEle.select("img");
            if (imgElements.size() > 0) {
                for (Element imgEle : imgElements) {
                    if (imgEle.hasAttr("data_ue__src")) {
                        Map<String, String> blockMap = new HashMap<>();
                        blockMap.put("type", "PICTURE");

                        if (imgEle.attr("data_ue__src").trim().startsWith("http")) {
                            blockMap.put("content", imgEle.attr("data_ue__src"));
                        } else {
                            blockMap.put("content", "http:" + imgEle.attr("data_ue__src"));
                        }
                        if (!StringUtil.isBlank(childEle.text().replace(" ", ""))) {
                            blockMap.put("POI", childEle.text().trim().replace(" ", ""));
                        }
                        contextBlocks.add(blockMap);
                    }
                }
            } else {
                if (!StringUtil.isBlank(childEle.text().replace(" ", ""))) {
                    contextBlocks.add(generateBlockMap(childEle.text().trim(), "TEXT"));
                }
            }
        }
        return contextBlocks;
    }

    public static Map<String, String> generateBlockMap(String content, String type) {
        Map<String, String> blockMap = new HashMap<>();
        blockMap.put("type", type);
        blockMap.put("content", content);
        return blockMap;
    }

    public static void main(String[] args) {
        String targetUrl = "http://go.ly.com/youji/2224311.html";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(extractNoteDetail(document));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateUrls() {
        String urls = "http://go.ly.com/youji/2224311.html\n" +
                "http://go.ly.com/travels/157000.html\n" +
                "http://go.ly.com/youji/2222772.html\n" +
                "http://go.ly.com/travels/148618.html\n" +
                "http://www.ly.com/travels/53002.html\n" +
                "http://www.ly.com/travels/173800.html\n" +
                "http://www.ly.com/travels/158092.html\n" +
                "http://www.ly.com/travels/144654.html\n" +
                "http://www.ly.com/travels/106062.html\n" +
                "http://www.ly.com/travels/50801.html\n" +
                "http://www.ly.com/travels/50822.html\n" +
                "http://www.ly.com/travels/50829.html\n" +
                "http://www.ly.com/travels/50840.html\n" +
                "http://www.ly.com/travels/50855.html\n" +
                "http://go.ly.com/youji/2219406.html\n" +
                "http://go.ly.com/youji/2204687.html\n" +
                "http://go.ly.com/youji/2204424.html\n" +
                "http://go.ly.com/youji/2197509.html\n" +
                "http://go.ly.com/youji/2196337.html\n" +
                "http://go.ly.com/youji/2192191.html\n" +
                "http://go.ly.com/youji/2189968.html\n" +
                "http://go.ly.com/youji/2184084.html\n" +
                "http://go.ly.com/youji/2182168.html\n" +
                "http://go.ly.com/youji/2179522.html\n" +
                "http://go.ly.com/youji/2178597.html\n" +
                "http://go.ly.com/youji/2175795.html\n" +
                "http://go.ly.com/youji/2174496.html\n" +
                "http://go.ly.com/youji/2166764.html\n" +
                "http://go.ly.com/youji/2166021.html\n" +
                "http://www.ly.com/travels/150511.html\n" +
                "http://www.ly.com/travels/59522.html\n" +
                "http://www.ly.com/travels/50301.html\n" +
                "http://www.ly.com/travels/50811.html\n" +
                "http://www.ly.com/travels/50802.html\n" +
                "http://www.ly.com/travels/50799.html\n" +
                "http://www.ly.com/travels/3049.html\n" +
                "http://www.ly.com/travels/3278.html\n" +
                "http://www.ly.com/travels/50813.html\n" +
                "http://www.ly.com/travels/21250.html\n" +
                "http://www.ly.com/travels/21108.html\n" +
                "http://www.ly.com/travels/2491.html\n" +
                "http://www.ly.com/travels/2619.html\n" +
                "http://www.ly.com/travels/2347.html\n" +
                "http://www.ly.com/travels/2929.html\n" +
                "http://www.ly.com/travels/2990.html\n" +
                "http://www.ly.com/travels/2355.html\n" +
                "http://www.ly.com/travels/2363.html\n" +
                "http://www.ly.com/travels/2369.html\n" +
                "http://www.ly.com/travels/2386.html\n" +
                "http://www.ly.com/travels/2455.html\n" +
                "http://www.ly.com/travels/2345.html\n" +
                "http://www.ly.com/travels/2356.html\n" +
                "http://www.ly.com/travels/2358.html\n" +
                "http://www.ly.com/travels/161393.html\n" +
                "http://www.ly.com/travels/151558.html\n" +
                "http://www.ly.com/travels/50777.html\n" +
                "http://www.ly.com/travels/47362.html\n" +
                "http://www.ly.com/travels/39796.html\n" +
                "http://www.ly.com/travels/46702.html\n" +
                "http://www.ly.com/travels/27439.html\n" +
                "http://www.ly.com/travels/46743.html\n" +
                "http://www.ly.com/travels/46752.html\n" +
                "http://www.ly.com/travels/47222.html\n" +
                "http://www.ly.com/travels/47233.html\n" +
                "http://www.ly.com/travels/47255.html\n" +
                "http://www.ly.com/travels/436.html\n" +
                "http://www.ly.com/travels/47288.html\n" +
                "http://www.ly.com/travels/494.html\n" +
                "http://www.ly.com/travels/47295.html\n" +
                "http://www.ly.com/travels/47312.html\n" +
                "http://www.ly.com/travels/47338.html\n" +
                "http://www.ly.com/travels/47357.html\n" +
                "http://www.ly.com/travels/47365.html\n" +
                "http://www.ly.com/travels/14726.html\n" +
                "http://www.ly.com/travels/11615.html\n" +
                "http://www.ly.com/travels/47372.html\n" +
                "http://www.ly.com/travels/47388.html\n" +
                "http://www.ly.com/travels/47505.html\n" +
                "http://www.ly.com/travels/47516.html\n" +
                "http://www.ly.com/travels/47537.html\n" +
                "http://www.ly.com/travels/24594.html\n" +
                "http://www.ly.com/travels/47553.html\n" +
                "http://go.ly.com/travels/161593.html\n" +
                "http://www.ly.com/travels/158569.html\n" +
                "http://www.ly.com/travels/151647.html\n" +
                "http://www.ly.com/travels/145502.html\n" +
                "http://www.ly.com/travels/32605.html\n" +
                "http://www.ly.com/travels/32610.html\n" +
                "http://www.ly.com/travels/621.html\n" +
                "http://www.ly.com/travels/32608.html\n" +
                "http://www.ly.com/travels/589.html\n" +
                "http://www.ly.com/travels/637.html\n" +
                "http://www.ly.com/travels/640.html\n" +
                "http://www.ly.com/travels/603.html\n" +
                "http://www.ly.com/travels/650.html\n" +
                "http://www.ly.com/travels/588.html\n" +
                "http://www.ly.com/travels/656.html\n" +
                "http://www.ly.com/travels/625.html\n" +
                "http://www.ly.com/travels/617.html\n" +
                "http://www.ly.com/travels/26489.html\n" +
                "http://www.ly.com/travels/32607.html\n" +
                "http://www.ly.com/travels/13299.html\n" +
                "http://www.ly.com/travels/32612.html\n" +
                "http://www.ly.com/travels/32618.html\n" +
                "http://www.ly.com/travels/32635.html\n" +
                "http://www.ly.com/travels/32677.html\n" +
                "http://www.ly.com/travels/46617.html\n" +
                "http://www.ly.com/travels/18192.html\n" +
                "http://www.ly.com/travels/33148.html\n" +
                "http://www.ly.com/travels/33145.html\n" +
                "http://www.ly.com/travels/33144.html\n" +
                "http://www.ly.com/travels/156014.html\n" +
                "http://www.ly.com/travels/148917.html\n" +
                "http://www.ly.com/travels/101810.html\n" +
                "http://www.ly.com/travels/110667.html\n" +
                "http://www.ly.com/travels/22197.html\n" +
                "http://www.ly.com/travels/12348.html\n" +
                "http://www.ly.com/travels/46433.html\n" +
                "http://www.ly.com/travels/46443.html\n" +
                "http://www.ly.com/travels/46455.html\n" +
                "http://www.ly.com/travels/46460.html\n" +
                "http://www.ly.com/travels/46464.html\n" +
                "http://go.ly.com/youji/2225687.html\n" +
                "http://go.ly.com/youji/2222764.html\n" +
                "http://go.ly.com/youji/2219434.html\n" +
                "http://go.ly.com/youji/2217404.html\n" +
                "http://go.ly.com/youji/2204225.html\n" +
                "http://go.ly.com/youji/2203710.html\n" +
                "http://go.ly.com/youji/2202127.html\n" +
                "http://go.ly.com/youji/2201586.html\n" +
                "http://go.ly.com/youji/2201245.html\n" +
                "http://go.ly.com/youji/2199530.html\n" +
                "http://go.ly.com/youji/2195868.html\n" +
                "http://go.ly.com/youji/2195682.html\n" +
                "http://go.ly.com/travels/145825.html\n" +
                "http://go.ly.com/travels/113973.html\n" +
                "http://go.ly.com/travels/113891.html\n" +
                "http://go.ly.com/travels/21831.html\n" +
                "http://www.ly.com/travels/160803.html\n" +
                "http://www.ly.com/travels/160912.html\n" +
                "http://www.ly.com/travels/159188.html\n" +
                "http://www.ly.com/travels/154827.html\n" +
                "http://www.ly.com/travels/155162.html\n" +
                "http://www.ly.com/travels/102339.html\n" +
                "http://www.ly.com/travels/108471.html\n" +
                "http://www.ly.com/travels/116597.html\n" +
                "http://www.ly.com/travels/65813.html\n" +
                "http://www.ly.com/travels/149482.html\n" +
                "http://www.ly.com/travels/65804.html\n" +
                "http://www.ly.com/travels/22373.html\n" +
                "http://www.ly.com/travels/65822.html\n" +
                "http://www.ly.com/travels/65852.html\n" +
                "http://www.ly.com/travels/50821.html\n" +
                "http://www.ly.com/travels/50805.html\n" +
                "http://www.ly.com/travels/65729.html\n" +
                "http://www.ly.com/travels/65750.html\n" +
                "http://go.ly.com/travels/119331.html \n" +
                "http://go.ly.com/travels/119497.html\n" +
                "http://www.ly.com/travels/200414.html\n" +
                "http://www.ly.com/travels/158232.html\n" +
                "http://www.ly.com/travels/151900.html\n" +
                "http://www.ly.com/travels/139095.html\n" +
                "http://www.ly.com/travels/113880.html\n" +
                "http://www.ly.com/travels/112115.html\n" +
                "http://www.ly.com/travels/50773.html\n" +
                "http://www.ly.com/travels/50776.html\n" +
                "http://www.ly.com/travels/39055.html\n" +
                "http://www.ly.com/travels/25516.html\n" +
                "http://www.ly.com/travels/12376.html\n" +
                "http://www.ly.com/travels/12298.html\n" +
                "http://www.ly.com/travels/12295.html\n" +
                "http://www.ly.com/travels/11287.html\n" +
                "http://www.ly.com/travels/161342.html\n" +
                "http://www.ly.com/travels/154687.html\n" +
                "http://www.ly.com/travels/154672.html\n" +
                "http://www.ly.com/travels/149316.html\n" +
                "http://www.ly.com/travels/122159.html\n" +
                "http://www.ly.com/travels/117792.html\n" +
                "http://www.ly.com/travels/81264.html\n" +
                "http://www.ly.com/travels/106607.html\n" +
                "http://www.ly.com/travels/105951.html\n" +
                "http://www.ly.com/travels/104013.html\n" +
                "http://www.ly.com/travels/91208.html\n" +
                "http://go.ly.com/youji/2201351.html\n" +
                "http://go.ly.com/youji/2172862.html\n" +
                "http://go.ly.com/youji/2189259.html\n" +
                "http://go.ly.com/youji/2179686.html\n" +
                "http://www.ly.com/travels/201629.html\n" +
                "http://www.ly.com/travels/158484.html\n" +
                "http://www.ly.com/travels/156429.html\n" +
                "http://www.ly.com/travels/147370.html\n" +
                "http://www.ly.com/travels/135020.html\n" +
                "http://www.ly.com/travels/113972.html\n" +
                "http://www.ly.com/travels/113974.html\n" +
                "http://www.ly.com/travels/113966.html\n" +
                "http://www.ly.com/travels/113963.html\n" +
                "http://www.ly.com/travels/113952.html\n" +
                "http://www.ly.com/travels/113879.html\n" +
                "http://www.ly.com/travels/102847.html\n" +
                "http://www.ly.com/travels/65358.html\n" +
                "http://go.ly.com/youji/2221785.html\n" +
                "http://go.ly.com/youji/2213268.html\n" +
                "http://go.ly.com/youji/2203559.html\n" +
                "http://go.ly.com/youji/2201679.html\n" +
                "http://go.ly.com/youji/2198722.html\n" +
                "http://go.ly.com/youji/2195406.html\n" +
                "http://go.ly.com/youji/2181542.html\n" +
                "http://go.ly.com/youji/2180851.html\n" +
                "http://go.ly.com/youji/2174064.html\n" +
                "http://go.ly.com/youji/2138185.html\n" +
                "http://go.ly.com/youji/2135395.html\n" +
                "http://go.ly.com/youji/1959905.html";
        for (String url : urls.split("\n")) {
            if (url.contains("youji")) {
                System.out.println("url:'" + url + "'");
            }
        }
    }
}
