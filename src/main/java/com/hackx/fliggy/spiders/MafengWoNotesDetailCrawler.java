package com.hackx.fliggy.spiders;

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

    public static List<Map<String, String>> getTravelNoteContent(Document document) {
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

    public static List<Map<String, String>> extractParagraph(Element articleTitle) {
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
                /*Document noteContentDoc = Jsoup.parse(nextElementSibling.html().replace("<br>", "\n"));*/
                contentBlock.put("content", nextElementSibling.text().replace(" ", "\n"));
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

    public static List<Map<String, String>> extractDefaultParagraph(Element jMasterContent) {
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
                /*Document noteContentDoc = Jsoup.parse(ithChild.html().replace("<br>", "\n"));*/
                contentBlock.put("content", ithChild.text().replace(" ", "\n"));
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

    public static void main(String[] args) {
        String targetUrl = "http://www.mafengwo.cn/i/6560927.html";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(extractNoteDetail(document));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateUrls(){
        String urls = "http://www.mafengwo.cn/i/6560927.html\n" +
                "http://www.mafengwo.cn/i/6560570.html\n" +
                "http://www.mafengwo.cn/i/6332396.html\n" +
                "http://www.mafengwo.cn/i/5626307.html\n" +
                "http://www.mafengwo.cn/i/5448404.html\n" +
                "http://www.mafengwo.cn/i/5405747.html\n" +
                "http://www.mafengwo.cn/i/5371899.html\n" +
                "http://www.mafengwo.cn/i/5366963.html\n" +
                "http://www.mafengwo.cn/i/3514316.html\n" +
                "http://www.mafengwo.cn/i/3475055.html\n" +
                "http://www.mafengwo.cn/i/3472063.html\n" +
                "http://www.mafengwo.cn/i/3468571.html\n" +
                "http://www.mafengwo.cn/i/3248374.html\n" +
                "http://www.mafengwo.cn/i/5578265.html\n" +
                "http://www.mafengwo.cn/i/5380590.html\n" +
                "http://www.mafengwo.cn/i/5378571.html\n" +
                "http://www.mafengwo.cn/i/5374890.html\n" +
                "http://www.mafengwo.cn/i/5367033.html\n" +
                "http://www.mafengwo.cn/i/3468602.html\n" +
                "http://www.mafengwo.cn/i/3402164.html\n" +
                "http://www.mafengwo.cn/i/3340689.html\n" +
                "http://www.mafengwo.cn/i/5608400.html\n" +
                "http://www.mafengwo.cn/i/6196016.html\n" +
                "http://www.mafengwo.cn/i/5655704.html\n" +
                "http://www.mafengwo.cn/i/5474415.html\n" +
                "http://www.mafengwo.cn/i/5349434.html\n" +
                "http://www.mafengwo.cn/i/6591227.html\n" +
                "http://www.mafengwo.cn/i/3479298.html\n" +
                "http://www.mafengwo.cn/i/3313548.html\n" +
                "http://www.mafengwo.cn/i/3313115.html\n" +
                "http://www.mafengwo.cn/i/1363612.html\n" +
                "http://www.mafengwo.cn/i/1030355.html\n" +
                "http://www.mafengwo.cn/i/5431396.html\n" +
                "http://www.mafengwo.cn/i/6254447.html\n" +
                "http://www.mafengwo.cn/i/5579051.html\n" +
                "http://www.mafengwo.cn/i/6191624.html\n" +
                "http://www.mafengwo.cn/i/5697980.html\n" +
                "http://www.mafengwo.cn/i/6537602.html\n" +
                "http://www.mafengwo.cn/i/6212021.html\n" +
                "http://www.mafengwo.cn/i/6135830.html\n" +
                "http://www.mafengwo.cn/i/5713592.html\n" +
                "http://www.mafengwo.cn/i/5531099.html\n" +
                "http://www.mafengwo.cn/i/5487462.html\n" +
                "http://www.mafengwo.cn/i/5473037.html\n" +
                "http://www.mafengwo.cn/i/5402505.html\n" +
                "http://www.mafengwo.cn/i/5357151.html\n" +
                "http://www.mafengwo.cn/i/5330211.html\n" +
                "http://www.mafengwo.cn/i/3481277.html\n" +
                "http://www.mafengwo.cn/i/3413505.html\n" +
                "http://www.mafengwo.cn/i/3309116.html\n" +
                "http://www.mafengwo.cn/i/3238689.html\n" +
                "http://www.mafengwo.cn/i/3112588.html\n" +
                "http://www.mafengwo.cn/i/3094992.html\n" +
                "http://www.mafengwo.cn/i/2865974.html\n" +
                "http://www.mafengwo.cn/i/6577193.html\n" +
                "http://www.mafengwo.cn/i/6324161.html\n" +
                "http://www.mafengwo.cn/i/6306125.html\n" +
                "http://www.mafengwo.cn/i/5406244.html\n" +
                "http://www.mafengwo.cn/i/5405357.html\n" +
                "http://www.mafengwo.cn/i/5397658.html\n" +
                "http://www.mafengwo.cn/i/5356647.html\n" +
                "http://www.mafengwo.cn/i/5352583.html\n" +
                "http://www.mafengwo.cn/i/5351581.html\n" +
                "http://www.mafengwo.cn/i/5347709.html\n" +
                "http://www.mafengwo.cn/i/5346504.html\n" +
                "http://www.mafengwo.cn/i/5346006.html\n" +
                "http://www.mafengwo.cn/i/5344903.html\n" +
                "http://www.mafengwo.cn/i/5344551.html\n" +
                "http://www.mafengwo.cn/i/5343506.html\n" +
                "http://www.mafengwo.cn/i/5335155.html\n" +
                "http://www.mafengwo.cn/i/3547761.html\n" +
                "http://www.mafengwo.cn/i/3510059.html\n" +
                "http://www.mafengwo.cn/i/3477246.html\n" +
                "http://www.mafengwo.cn/i/3475823.html\n" +
                "http://www.mafengwo.cn/i/3470629.html\n" +
                "http://www.mafengwo.cn/i/3464071.html\n" +
                "http://www.mafengwo.cn/i/5516920.html\n" +
                "http://www.mafengwo.cn/i/6332264.html\n" +
                "http://www.mafengwo.cn/i/6298721.html\n" +
                "http://www.mafengwo.cn/i/5486640.html\n" +
                "http://www.mafengwo.cn/i/5449539.html\n" +
                "http://www.mafengwo.cn/i/5324829.html\n" +
                "http://www.mafengwo.cn/i/3093108.html\n" +
                "http://www.mafengwo.cn/i/2994117.html\n" +
                "http://www.mafengwo.cn/i/6539153.html\n" +
                "http://www.mafengwo.cn/i/6539123.html\n" +
                "http://www.mafengwo.cn/i/6539066.html\n" +
                "http://www.mafengwo.cn/i/6530096.html\n" +
                "http://www.mafengwo.cn/i/6530084.html\n" +
                "http://www.mafengwo.cn/i/6530075.html\n" +
                "http://www.mafengwo.cn/i/6528119.html\n" +
                "http://www.mafengwo.cn/i/6528122.html\n" +
                "http://www.mafengwo.cn/i/6528113.html\n" +
                "http://www.mafengwo.cn/i/6527891.html\n" +
                "http://www.mafengwo.cn/i/6516197.html\n" +
                "http://www.mafengwo.cn/i/6494516.html\n" +
                "http://www.mafengwo.cn/i/2840890.html\n" +
                "http://www.mafengwo.cn/i/1245270.html\n" +
                "http://www.mafengwo.cn/i/1233949.html\n" +
                "http://www.mafengwo.cn/i/1172959.html\n" +
                "http://www.mafengwo.cn/i/1161078.html\n" +
                "http://www.mafengwo.cn/i/1159213.html\n" +
                "http://www.mafengwo.cn/i/1157470.html\n" +
                "http://www.mafengwo.cn/i/1137808.html\n" +
                "http://www.mafengwo.cn/i/1112238.html\n" +
                "http://www.mafengwo.cn/i/979312.html\n" +
                "http://www.mafengwo.cn/i/963744.html\n" +
                "http://www.mafengwo.cn/i/947197.html\n" +
                "http://www.mafengwo.cn/i/935342.html\n" +
                "http://www.mafengwo.cn/i/933547.html\n" +
                "http://www.mafengwo.cn/i/932344.html\n" +
                "http://www.mafengwo.cn/i/931384.html\n" +
                "http://www.mafengwo.cn/i/923059.html\n" +
                "http://www.mafengwo.cn/i/906053.html\n" +
                "http://www.mafengwo.cn/i/886952.html\n" +
                "http://www.mafengwo.cn/i/885482.html\n" +
                "http://www.mafengwo.cn/i/885466.html\n" +
                "http://www.mafengwo.cn/i/884095.html\n" +
                "http://www.mafengwo.cn/i/884072.html\n" +
                "http://www.mafengwo.cn/i/883920.html\n" +
                "http://www.mafengwo.cn/i/881221.html\n" +
                "http://www.mafengwo.cn/i/881202.html\n" +
                "http://www.mafengwo.cn/i/881178.html\n" +
                "http://www.mafengwo.cn/i/881133.html\n" +
                "http://www.mafengwo.cn/i/880793.html\n" +
                "http://www.mafengwo.cn/i/880772.html\n" +
                "http://www.mafengwo.cn/i/880709.html\n" +
                "http://www.mafengwo.cn/i/880657.html\n" +
                "http://www.mafengwo.cn/i/880600.html\n" +
                "http://www.mafengwo.cn/i/880491.html\n" +
                "http://www.mafengwo.cn/i/880451.html\n" +
                "http://www.mafengwo.cn/i/877413.html\n" +
                "http://www.mafengwo.cn/i/877396.html\n" +
                "http://www.mafengwo.cn/i/872192.html\n" +
                "http://www.mafengwo.cn/i/872085.html\n" +
                "http://www.mafengwo.cn/i/872065.html\n" +
                "http://www.mafengwo.cn/i/737992.html\n" +
                "http://www.mafengwo.cn/i/737563.html\n" +
                "http://www.mafengwo.cn/i/724769.html\n" +
                "http://www.mafengwo.cn/i/719688.html\n" +
                "http://www.mafengwo.cn/i/719129.html\n" +
                "http://www.mafengwo.cn/i/716883.html\n" +
                "http://www.mafengwo.cn/i/712873.html\n" +
                "http://www.mafengwo.cn/i/712862.html\n" +
                "http://www.mafengwo.cn/i/712856.html\n" +
                "http://www.mafengwo.cn/i/712837.html\n" +
                "http://www.mafengwo.cn/i/712764.html\n" +
                "http://www.mafengwo.cn/i/709300.html\n" +
                "http://www.mafengwo.cn/i/709286.html\n" +
                "http://www.mafengwo.cn/i/709279.html\n" +
                "http://www.mafengwo.cn/i/693952.html\n" +
                "http://www.mafengwo.cn/i/6257375.html\n" +
                "http://www.mafengwo.cn/i/5545200.html\n" +
                "http://www.mafengwo.cn/i/5541606.html\n" +
                "http://www.mafengwo.cn/i/5537306.html\n" +
                "http://www.mafengwo.cn/i/5533123.html\n" +
                "http://www.mafengwo.cn/i/5507960.html\n" +
                "http://www.mafengwo.cn/i/5477405.html\n" +
                "http://www.mafengwo.cn/i/5328618.html\n" +
                "http://www.mafengwo.cn/i/3429998.html\n" +
                "http://www.mafengwo.cn/i/3274637.html\n" +
                "http://www.mafengwo.cn/i/3254921.html\n" +
                "http://www.mafengwo.cn/i/3151573.html\n" +
                "http://www.mafengwo.cn/i/3125016.html\n" +
                "http://www.mafengwo.cn/i/3032760.html\n" +
                "http://www.mafengwo.cn/i/1377392.html\n" +
                "http://www.mafengwo.cn/i/5579177.html\n" +
                "http://www.mafengwo.cn/i/6568136.html\n" +
                "http://www.mafengwo.cn/i/5500407.html\n" +
                "http://www.mafengwo.cn/i/5339843.html\n" +
                "http://www.mafengwo.cn/i/3472637.html\n" +
                "http://www.mafengwo.cn/i/3313576.html\n" +
                "http://www.mafengwo.cn/i/5332516.html\n" +
                "http://www.mafengwo.cn/i/5369004.html\n" +
                "http://www.mafengwo.cn/i/6305060.html\n" +
                "http://www.mafengwo.cn/i/5391785.html\n" +
                "http://www.mafengwo.cn/i/5340759.html\n" +
                "http://www.mafengwo.cn/i/3327099.html\n" +
                "http://www.mafengwo.cn/i/3091816.html\n" +
                "http://www.mafengwo.cn/i/2869300.html\n" +
                "http://www.mafengwo.cn/i/5559470.html\n" +
                "http://www.mafengwo.cn/i/5432145.html\n" +
                "http://www.mafengwo.cn/i/6301907.html\n" +
                "http://www.mafengwo.cn/i/6184937.html\n" +
                "http://www.mafengwo.cn/i/5672870.html\n" +
                "http://www.mafengwo.cn/i/5557244.html\n" +
                "http://www.mafengwo.cn/i/5541817.html\n" +
                "http://www.mafengwo.cn/i/5530254.html\n" +
                "http://www.mafengwo.cn/i/3522907.html\n" +
                "http://www.mafengwo.cn/i/3466493.html\n" +
                "http://www.mafengwo.cn/i/3456754.html\n" +
                "http://www.mafengwo.cn/i/3446663.html\n" +
                "http://www.mafengwo.cn/i/6200330.html\n" +
                "http://www.mafengwo.cn/i/5453091.html\n" +
                "http://www.mafengwo.cn/i/5380564.html\n" +
                "http://www.mafengwo.cn/i/3537568.html\n" +
                "http://www.mafengwo.cn/i/3523364.html\n" +
                "http://www.mafengwo.cn/i/3513074.html\n" +
                "http://www.mafengwo.cn/i/3417205.html\n" +
                "http://www.mafengwo.cn/i/3311145.html\n" +
                "http://www.mafengwo.cn/i/3215021.html\n" +
                "http://www.mafengwo.cn/i/3078723.html\n" +
                "http://www.mafengwo.cn/i/2964902.html\n" +
                "http://www.mafengwo.cn/i/2873860.html\n" +
                "http://www.mafengwo.cn/i/1347835.html\n" +
                "http://www.mafengwo.cn/i/5507788.html\n" +
                "http://www.mafengwo.cn/i/5438956.html\n" +
                "http://www.mafengwo.cn/i/5429889.html\n" +
                "http://www.mafengwo.cn/i/5368156.html\n" +
                "http://www.mafengwo.cn/i/5359837.html\n" +
                "http://www.mafengwo.cn/i/3315345.html\n" +
                "http://www.mafengwo.cn/i/3274885.html\n" +
                "http://www.mafengwo.cn/i/3253583.html\n" +
                "http://www.mafengwo.cn/i/3220567.html\n" +
                "http://www.mafengwo.cn/i/2909219.html\n" +
                "http://www.mafengwo.cn/i/5597942.html\n" +
                "http://www.mafengwo.cn/i/5582834.html\n" +
                "http://www.mafengwo.cn/i/5576813.html\n" +
                "http://www.mafengwo.cn/i/5555378.html\n" +
                "http://www.mafengwo.cn/i/5459988.html\n" +
                "http://www.mafengwo.cn/i/5443512.html\n" +
                "http://www.mafengwo.cn/i/5366809.html\n" +
                "http://www.mafengwo.cn/i/5356816.html\n" +
                "http://www.mafengwo.cn/i/3458400.html\n" +
                "http://www.mafengwo.cn/i/3396540.html\n" +
                "http://www.mafengwo.cn/i/3388392.html\n" +
                "http://www.mafengwo.cn/i/3353636.html\n" +
                "http://www.mafengwo.cn/i/3163194.html\n" +
                "http://www.mafengwo.cn/i/3094372.html\n" +
                "http://www.mafengwo.cn/i/3018561.html\n" +
                "http://www.mafengwo.cn/i/2973675.html\n" +
                "http://www.mafengwo.cn/i/2901024.html\n" +
                "http://www.mafengwo.cn/i/6523889.html\n" +
                "http://www.mafengwo.cn/i/6139424.html\n" +
                "http://www.mafengwo.cn/i/5551733.html\n" +
                "http://www.mafengwo.cn/i/5463446.html\n" +
                "http://www.mafengwo.cn/i/5345774.html\n" +
                "http://www.mafengwo.cn/i/6355346.html\n" +
                "http://www.mafengwo.cn/i/6346994.html\n" +
                "http://www.mafengwo.cn/i/6345332.html\n" +
                "http://www.mafengwo.cn/i/5670599.html\n" +
                "http://www.mafengwo.cn/i/5663801.html\n" +
                "http://www.mafengwo.cn/i/5375821.html\n" +
                "http://www.mafengwo.cn/i/5365972.html\n" +
                "http://www.mafengwo.cn/i/5337444.html\n" +
                "http://www.mafengwo.cn/i/5318753.html\n" +
                "http://www.mafengwo.cn/i/5317943.html\n" +
                "http://www.mafengwo.cn/i/3458499.html\n" +
                "http://www.mafengwo.cn/i/3339026.html\n" +
                "http://www.mafengwo.cn/i/3269381.html\n" +
                "http://www.mafengwo.cn/i/2955623.html\n" +
                "http://www.mafengwo.cn/i/2953684.html\n" +
                "http://www.mafengwo.cn/i/6594611.html\n" +
                "http://www.mafengwo.cn/i/5477440.html\n" +
                "http://www.mafengwo.cn/i/3338484.html\n" +
                "http://www.mafengwo.cn/i/3115833.html\n" +
                "http://www.mafengwo.cn/i/3101753.html\n" +
                "http://www.mafengwo.cn/i/3017643.html\n" +
                "http://www.mafengwo.cn/i/3013125.html\n" +
                "http://www.mafengwo.cn/i/2948606.html\n" +
                "http://www.mafengwo.cn/i/2878924.html\n" +
                "http://www.mafengwo.cn/i/2843795.html\n" +
                "http://www.mafengwo.cn/i/1354515.html\n" +
                "http://www.mafengwo.cn/i/1164801.html\n" +
                "http://www.mafengwo.cn/i/1141922.html\n" +
                "http://www.mafengwo.cn/i/1124181.html\n" +
                "http://www.mafengwo.cn/i/1043747.html\n" +
                "http://www.mafengwo.cn/i/1017670.html\n" +
                "http://www.mafengwo.cn/i/961764.html\n" +
                "http://www.mafengwo.cn/i/925524.html\n" +
                "http://www.mafengwo.cn/i/925493.html\n" +
                "http://www.mafengwo.cn/i/875937.html\n" +
                "http://www.mafengwo.cn/i/856433.html\n" +
                "http://www.mafengwo.cn/i/839166.html\n" +
                "http://www.mafengwo.cn/i/833083.html\n" +
                "http://www.mafengwo.cn/i/830538.html\n" +
                "http://www.mafengwo.cn/i/830535.html\n" +
                "http://www.mafengwo.cn/i/830324.html\n" +
                "http://www.mafengwo.cn/i/828859.html\n" +
                "http://www.mafengwo.cn/i/828247.html\n" +
                "http://www.mafengwo.cn/i/827652.html\n" +
                "http://www.mafengwo.cn/i/827028.html\n" +
                "http://www.mafengwo.cn/i/826694.html\n" +
                "http://www.mafengwo.cn/i/826684.html\n" +
                "http://www.mafengwo.cn/i/826661.html\n" +
                "http://www.mafengwo.cn/i/826655.html\n" +
                "http://www.mafengwo.cn/i/826633.html\n" +
                "http://www.mafengwo.cn/i/826615.html\n" +
                "http://www.mafengwo.cn/i/5372331.html\n" +
                "http://www.mafengwo.cn/i/3344101.html\n" +
                "http://www.mafengwo.cn/i/3385536.html\n" +
                "http://www.mafengwo.cn/i/3472757.html\n" +
                "http://www.mafengwo.cn/i/3461927.html\n" +
                "http://www.mafengwo.cn/i/6582404.html\n" +
                "http://www.mafengwo.cn/i/6180803.html\n" +
                "http://www.mafengwo.cn/i/5582465.html\n" +
                "http://www.mafengwo.cn/i/3505916.html\n" +
                "http://www.mafengwo.cn/i/3327299.html\n" +
                "http://www.mafengwo.cn/i/3273419.html\n" +
                "http://www.mafengwo.cn/i/3176926.html\n" +
                "http://www.mafengwo.cn/i/3098169.html\n" +
                "http://www.mafengwo.cn/i/2975544.html\n" +
                "http://www.mafengwo.cn/i/2964898.html\n" +
                "http://www.mafengwo.cn/i/2942487.html\n" +
                "http://www.mafengwo.cn/i/2870855.html\n" +
                "http://www.mafengwo.cn/i/2867982.html\n" +
                "http://www.mafengwo.cn/i/1265298.html\n" +
                "http://www.mafengwo.cn/i/1238709.html\n" +
                "http://www.mafengwo.cn/i/1156389.html\n" +
                "http://www.mafengwo.cn/i/1112524.html\n" +
                "http://www.mafengwo.cn/i/867073.html\n" +
                "http://www.mafengwo.cn/i/857175.html\n" +
                "http://www.mafengwo.cn/i/854890.html\n" +
                "http://www.mafengwo.cn/i/847046.html\n" +
                "http://www.mafengwo.cn/i/3048353.html\n" +
                "http://www.mafengwo.cn/i/2984371.html\n" +
                "http://www.mafengwo.cn/i/5610407.html\n" +
                "http://www.mafengwo.cn/i/5448027.html\n" +
                "http://www.mafengwo.cn/i/5343657.html\n" +
                "http://www.mafengwo.cn/i/3242900.html\n" +
                "http://www.mafengwo.cn/i/6580586.html\n" +
                "http://www.mafengwo.cn/i/6563663.html\n" +
                "http://www.mafengwo.cn/i/6547091.html\n" +
                "http://www.mafengwo.cn/i/6536834.html\n" +
                "http://www.mafengwo.cn/i/6536378.html\n" +
                "http://www.mafengwo.cn/i/3339271.html\n" +
                "http://www.mafengwo.cn/i/5370456.html\n" +
                "http://www.mafengwo.cn/i/6334349.html\n" +
                "http://www.mafengwo.cn/i/5568104.html\n" +
                "http://www.mafengwo.cn/i/5547145.html\n" +
                "http://www.mafengwo.cn/i/5539158.html\n" +
                "http://www.mafengwo.cn/i/5529357.html\n" +
                "http://www.mafengwo.cn/i/5448355.html\n" +
                "http://www.mafengwo.cn/i/5404245.html\n" +
                "http://www.mafengwo.cn/i/5374003.html\n" +
                "http://www.mafengwo.cn/i/5360835.html\n" +
                "http://www.mafengwo.cn/i/5330670.html\n" +
                "http://www.mafengwo.cn/i/3480731.html\n" +
                "http://www.mafengwo.cn/i/3284840.html\n" +
                "http://www.mafengwo.cn/i/3128613.html\n" +
                "http://www.mafengwo.cn/i/3021797.html\n" +
                "http://www.mafengwo.cn/i/1318913.html\n" +
                "http://www.mafengwo.cn/i/1175713.html\n" +
                "http://www.mafengwo.cn/i/1141497.html\n" +
                "http://www.mafengwo.cn/i/1133161.html\n" +
                "http://www.mafengwo.cn/i/1126461.html\n" +
                "http://www.mafengwo.cn/i/984661.html\n" +
                "http://www.mafengwo.cn/i/932661.html\n" +
                "http://www.mafengwo.cn/i/922592.html\n" +
                "http://www.mafengwo.cn/i/919732.html\n" +
                "http://www.mafengwo.cn/i/914403.html\n" +
                "http://www.mafengwo.cn/i/6499685.html\n" +
                "http://www.mafengwo.cn/i/6195206.html\n" +
                "http://www.mafengwo.cn/i/5517787.html\n" +
                "http://www.mafengwo.cn/i/5476983.html\n" +
                "http://www.mafengwo.cn/i/5362313.html\n" +
                "http://www.mafengwo.cn/i/6372389.html\n" +
                "http://www.mafengwo.cn/i/5422102.html\n" +
                "http://www.mafengwo.cn/i/3466643.html\n" +
                "http://www.mafengwo.cn/i/3318569.html\n" +
                "http://www.mafengwo.cn/i/3280315.html\n" +
                "http://www.mafengwo.cn/i/3102777.html\n" +
                "http://www.mafengwo.cn/i/2883197.html\n" +
                "http://www.mafengwo.cn/i/6242252.html\n" +
                "http://www.mafengwo.cn/i/5661626.html\n" +
                "http://www.mafengwo.cn/i/5458975.html\n" +
                "http://www.mafengwo.cn/i/3507857.html\n" +
                "http://www.mafengwo.cn/i/3418329.html\n" +
                "http://www.mafengwo.cn/i/3315619.html\n" +
                "http://www.mafengwo.cn/i/3173271.html\n" +
                "http://www.mafengwo.cn/i/3123366.html\n" +
                "http://www.mafengwo.cn/i/3114852.html\n" +
                "http://www.mafengwo.cn/i/3071182.html\n" +
                "http://www.mafengwo.cn/i/3071097.html\n" +
                "http://www.mafengwo.cn/i/3028149.html\n" +
                "http://www.mafengwo.cn/i/3016461.html\n" +
                "http://www.mafengwo.cn/i/2991452.html\n" +
                "http://www.mafengwo.cn/i/6339866.html\n" +
                "http://www.mafengwo.cn/i/6176900.html\n" +
                "http://www.mafengwo.cn/i/5532180.html\n" +
                "http://www.mafengwo.cn/i/5443801.html\n" +
                "http://www.mafengwo.cn/i/5372429.html\n" +
                "http://www.mafengwo.cn/i/3521613.html\n" +
                "http://www.mafengwo.cn/i/3473240.html\n" +
                "http://www.mafengwo.cn/i/3275913.html\n" +
                "http://www.mafengwo.cn/i/5478450.html\n" +
                "http://www.mafengwo.cn/i/5382697.html\n" +
                "http://www.mafengwo.cn/i/5322242.html\n" +
                "http://www.mafengwo.cn/i/3445453.html\n" +
                "http://www.mafengwo.cn/i/3309185.html\n" +
                "http://www.mafengwo.cn/i/3292807.html\n" +
                "http://www.mafengwo.cn/i/6282434.html\n" +
                "http://www.mafengwo.cn/i/5453521.html\n" +
                "http://www.mafengwo.cn/i/3506314.html\n" +
                "http://www.mafengwo.cn/i/5449531.html\n" +
                "http://www.mafengwo.cn/i/5406665.html\n" +
                "http://www.mafengwo.cn/i/5406658.html\n" +
                "http://www.mafengwo.cn/i/5360701.html\n" +
                "http://www.mafengwo.cn/i/3524813.html\n" +
                "http://www.mafengwo.cn/i/3484307.html\n" +
                "http://www.mafengwo.cn/i/3466378.html\n" +
                "http://www.mafengwo.cn/i/3443669.html\n" +
                "http://www.mafengwo.cn/i/3441197.html\n" +
                "http://www.mafengwo.cn/i/3429689.html\n" +
                "http://www.mafengwo.cn/i/3422205.html\n" +
                "http://www.mafengwo.cn/i/3323076.html\n" +
                "http://www.mafengwo.cn/i/3321082.html\n" +
                "http://www.mafengwo.cn/i/3248924.html\n" +
                "http://www.mafengwo.cn/i/3243102.html\n" +
                "http://www.mafengwo.cn/i/5382074.html\n" +
                "http://www.mafengwo.cn/i/6534086.html\n" +
                "http://www.mafengwo.cn/i/6293549.html\n" +
                "http://www.mafengwo.cn/i/5540909.html\n" +
                "http://www.mafengwo.cn/i/5504893.html\n" +
                "http://www.mafengwo.cn/i/5463130.html\n" +
                "http://www.mafengwo.cn/i/5347008.html\n" +
                "http://www.mafengwo.cn/i/3508584.html\n" +
                "http://www.mafengwo.cn/i/3442261.html\n" +
                "http://www.mafengwo.cn/i/3404314.html\n" +
                "http://www.mafengwo.cn/i/3313892.html\n" +
                "http://www.mafengwo.cn/i/3250891.html\n" +
                "http://www.mafengwo.cn/i/3223355.html\n" +
                "http://www.mafengwo.cn/i/3088837.html\n" +
                "http://www.mafengwo.cn/i/3035582.html\n" +
                "http://www.mafengwo.cn/i/1112490.html\n" +
                "http://www.mafengwo.cn/i/5500236.html\n" +
                "http://www.mafengwo.cn/i/6517865.html\n" +
                "http://www.mafengwo.cn/i/5617208.html\n" +
                "http://www.mafengwo.cn/i/5333447.html\n" +
                "http://www.mafengwo.cn/i/5318699.html\n" +
                "http://www.mafengwo.cn/i/3116888.html\n" +
                "http://www.mafengwo.cn/i/1293355.html\n" +
                "http://www.mafengwo.cn/i/1175187.html\n" +
                "http://www.mafengwo.cn/i/1107202.html\n" +
                "http://www.mafengwo.cn/i/1021814.html\n" +
                "http://www.mafengwo.cn/i/862942.html\n" +
                "http://www.mafengwo.cn/i/6614306.html\n" +
                "http://www.mafengwo.cn/i/6551096.html\n" +
                "http://www.mafengwo.cn/i/6290576.html\n" +
                "http://www.mafengwo.cn/i/5456243.html\n" +
                "http://www.mafengwo.cn/i/5438203.html\n" +
                "http://www.mafengwo.cn/i/5351033.html\n" +
                "http://www.mafengwo.cn/i/5336502.html\n" +
                "http://www.mafengwo.cn/i/3390834.html\n" +
                "http://www.mafengwo.cn/i/3244026.html\n" +
                "http://www.mafengwo.cn/i/3172925.html\n" +
                "http://www.mafengwo.cn/i/3030758.html\n" +
                "http://www.mafengwo.cn/i/1378465.html\n" +
                "http://www.mafengwo.cn/i/1341614.html\n" +
                "http://www.mafengwo.cn/i/1338443.html\n" +
                "http://www.mafengwo.cn/i/1327993.html\n" +
                "http://www.mafengwo.cn/i/1168131.html\n" +
                "http://www.mafengwo.cn/i/1160336.html\n" +
                "http://www.mafengwo.cn/i/1152081.html\n" +
                "http://www.mafengwo.cn/i/1077251.html\n" +
                "http://www.mafengwo.cn/i/1052944.html\n" +
                "http://www.mafengwo.cn/i/1037893.html\n" +
                "http://www.mafengwo.cn/i/967400.html";
        for (String url : urls.split("\n")){
            System.out.println("url:'" + url + "'");
        }
    }

    public JSONArray doExtract(String html, JSONObject param, List<String> warningList) {
        Document document = TrspExtractUtils.toDocument(html);
        JSONArray noteDetail = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", extractNoteDetail(document));
        noteDetail.add(jsonObject);
        return noteDetail;
    }

}
