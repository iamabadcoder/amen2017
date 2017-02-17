package com.hackx.spiders;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.extractor.domain.exception.TrspExtractException;
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

public class TuNiuNotesDetailCrawler extends TrspCrawlerExtractorAdapter {

    @Override
    protected JSONArray doExtract(String html, JSONObject param, List<String> list) throws TrspExtractException {
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

    public static List<Map<String, String>> getTravelNoteContent(Document document) {
        List<Map<String, String>> travelNotesContentSections = new ArrayList<>();
        try {
            Element divContentLeft = document.select("div.content-left").first();
            for (Element childEle : divContentLeft.children()) {
                if (childEle.className().contains("yj_table")) {
                    continue;
                } else if (childEle.className().contains("count-content") || childEle.className().contains("content-comment")) {
                    break;
                }
                Map<String, String> blockMap = extractSpecialParagraph(childEle);
                if (null != blockMap && blockMap.size() > 0){
                    travelNotesContentSections.add(blockMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return travelNotesContentSections;
    }

    public static Map<String, String> getTravelNoteAttribute(Document document) {
        Map<String, String> travelNoteAttribute = new HashMap<>();
        travelNoteAttribute.put("type", "NOTE_ATTRIBUTE");
        travelNoteAttribute.put("link", document.baseUri());
        /* 文章标题 */
        Element detailTitleEle = document.select(".headtext").first();
        if (null != detailTitleEle) {
            travelNoteAttribute.put("noteTitle", detailTitleEle.text());
        }
        /* 达人名称 */
        Element autherNameEle = document.select("div.auther-name").first();
        if (null != autherNameEle) {
            travelNoteAttribute.put("autherName", autherNameEle.select("a").first().ownText());
        }
        /* 发表时间 */
        Element autherPublishEle = document.select("div.auther-publish").first();
        if (null != autherPublishEle && null != autherPublishEle.select(".time").first()) {
            Element publishTimeEle = autherPublishEle.select(".time").first();
            travelNoteAttribute.put("publishTime", publishTimeEle.ownText().replace("发表于:", ""));
        }
        /* 达人头像 */
        Element autherImgEle = document.select("div.auther-img").first();
        if (null != autherImgEle && null != autherImgEle.select("img").first()) {
            Element imgEle = autherImgEle.select("img").first();
            travelNoteAttribute.put("authorPic", imgEle.attr("src"));
        }
        return travelNoteAttribute;
    }

    public static Map<String, String> extractSpecialParagraph(Element childElement) {
        Map<String, String> sectionMap = new HashMap<>();
        if (childElement.className().contains("section-tit")) {
            sectionMap.put("type", "FIRST_LEVEL_TITLE");
            sectionMap.put("content", childElement.text());
        } else if (childElement.className().contains("section-img")) {
            Elements imgElements = childElement.getElementsByTag("img");
            if (null != imgElements && imgElements.size() >= 1) {
                sectionMap.put("content", imgElements.first().attr("data-src"));
                sectionMap.put("type", "PICTURE");
            }
            /* 图片POI */
            Elements sectionPoiElements = childElement.getElementsByClass("section-img-poi");
            if (null != sectionPoiElements && sectionPoiElements.size() >= 1) {
                sectionMap.put("POI", sectionPoiElements.first().text());
            }
        } else if (childElement.className().contains("section-des")) {
            if (!StringUtil.isBlank(childElement.text().trim())) {
                sectionMap.put("content", childElement.text());
                sectionMap.put("type", "TEXT");
            }
        }
        return sectionMap;
    }

    public static List<Map<String, String>> extractDefaultParagraph(Element divContentLeft) {
        List<Map<String, String>> defaultParagraphList = new ArrayList<>();
        defaultParagraphList.add(generateParagraphTitle(null));
        Elements sectionsElements = divContentLeft.children();
        for (Element sectionEle : sectionsElements) {
            if (sectionEle.className().contains("section-tit")
                    || sectionEle.className().contains("count-content")
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
        titleBlock.put("type", "FIRST_LEVEL_TITLE");
        return titleBlock;
    }

    public static void generateUrls() {
        String urls = "http://www.tuniu.com/trips/12462654\n" +
                "http://www.tuniu.com/trips/10507133\n" +
                "http://www.tuniu.com/trips/10507130\n" +
                "http://www.tuniu.com/trips/10100825\n" +
                "http://www.tuniu.com/trips/10095518\n" +
                "http://www.tuniu.com/trips/10095511\n" +
                "http://www.tuniu.com/trips/10095501\n" +
                "http://www.tuniu.com/trips/10093196\n" +
                "http://www.tuniu.com/trips/10502485\n" +
                "http://www.tuniu.com/trips/10502484\n" +
                "http://www.tuniu.com/trips/10111642\n" +
                "http://www.tuniu.com/trips/10103912\n" +
                "http://www.tuniu.com/trips/10100359\n" +
                "http://www.tuniu.com/trips/10095560\n" +
                "http://www.tuniu.com/trips/10094306\n" +
                "http://www.tuniu.com/trips/10083411\n" +
                "http://www.tuniu.com/trips/10076438\n" +
                "http://www.tuniu.com/trips/10069678\n" +
                "http://www.tuniu.com/trips/10068622\n" +
                "http://www.tuniu.com/trips/10063634\n" +
                "http://www.tuniu.com/trips/10061511\n" +
                "http://www.tuniu.com/trips/10054036\n" +
                "http://www.tuniu.com/trips/10052819\n" +
                "http://www.tuniu.com/trips/12458557\n" +
                "http://www.tuniu.com/trips/10110753\n" +
                "http://www.tuniu.com/trips/10111187\n" +
                "http://www.tuniu.com/trips/10089862\n" +
                "http://www.tuniu.com/trips/10079860\n" +
                "http://www.tuniu.com/trips/10078337\n" +
                "http://www.tuniu.com/trips/10078291\n" +
                "http://www.tuniu.com/trips/10088139\n" +
                "http://www.tuniu.com/trips/10088486\n" +
                "http://www.tuniu.com/trips/10088501\n" +
                "http://www.tuniu.com/trips/10089669\n" +
                "http://www.tuniu.com/trips/10092574\n" +
                "http://www.tuniu.com/trips/10092977\n" +
                "http://www.tuniu.com/trips/10101420\n" +
                "http://www.tuniu.com/trips/10101520\n" +
                "http://www.tuniu.com/trips/10101842\n" +
                "http://www.tuniu.com/trips/10101949\n" +
                "http://www.tuniu.com/trips/10102043\n" +
                "http://www.tuniu.com/trips/10103100\n" +
                "http://www.tuniu.com/trips/10103454\n" +
                "http://www.tuniu.com/trips/10103560\n" +
                "http://www.tuniu.com/trips/10105014\n" +
                "http://www.tuniu.com/trips/10105016\n" +
                "http://www.tuniu.com/trips/10105112\n" +
                "http://www.tuniu.com/trips/10105118\n" +
                "http://www.tuniu.com/trips/10106473\n" +
                "http://www.tuniu.com/trips/10109334\n" +
                "http://www.tuniu.com/trips/10109461\n" +
                "http://www.tuniu.com/trips/10501049\n" +
                "http://www.tuniu.com/trips/10501963\n" +
                "http://www.tuniu.com/trips/10505212\n" +
                "http://www.tuniu.com/trips/10506433\n" +
                "http://www.tuniu.com/trips/10506531\n" +
                "http://www.tuniu.com/trips/12458025\n" +
                "http://www.tuniu.com/trips/12458225\n" +
                "http://www.tuniu.com/trips/12535997\n" +
                "http://www.tuniu.com/trips/12536004\n" +
                "http://www.tuniu.com/trips/12461923\n" +
                "http://www.tuniu.com/trips/12463530\n" +
                "http://www.tuniu.com/trips/12535553\n" +
                "http://www.tuniu.com/trips/12461832\n" +
                "http://www.tuniu.com/trips/12458831\n" +
                "http://www.tuniu.com/trips/12461371\n" +
                "http://www.tuniu.com/trips/12461827\n" +
                "http://www.tuniu.com/trips/10081804\n" +
                "http://www.tuniu.com/trips/10088273\n" +
                "http://www.tuniu.com/trips/10092194\n" +
                "http://www.tuniu.com/trips/10094326\n" +
                "http://www.tuniu.com/trips/10106901\n" +
                "http://www.tuniu.com/trips/10085623\n" +
                "http://www.tuniu.com/trips/10109810\n" +
                "http://www.tuniu.com/trips/12460943\n" +
                "http://www.tuniu.com/trips/10503933\n" +
                "http://www.tuniu.com/trips/10501972\n" +
                "http://www.tuniu.com/trips/12457545\n" +
                "http://www.tuniu.com/trips/12461233\n" +
                "http://www.tuniu.com/trips/12459120\n" +
                "http://www.tuniu.com/trips/10501776\n" +
                "http://www.tuniu.com/trips/10086403\n" +
                "http://www.tuniu.com/trips/10079708\n" +
                "http://www.tuniu.com/trips/10080004\n" +
                "http://www.tuniu.com/trips/10080434\n" +
                "http://www.tuniu.com/trips/10103240\n" +
                "http://www.tuniu.com/trips/12464367\n" +
                "http://www.tuniu.com/trips/12460450\n" +
                "http://www.tuniu.com/trips/12460580\n" +
                "http://www.tuniu.com/trips/12461015\n" +
                "http://www.tuniu.com/trips/12461192\n" +
                "http://www.tuniu.com/trips/12461194\n" +
                "http://www.tuniu.com/trips/12461415\n" +
                "http://www.tuniu.com/trips/12461463\n" +
                "http://www.tuniu.com/trips/12461581\n" +
                "http://www.tuniu.com/trips/12463211\n" +
                "http://www.tuniu.com/trips/12463599\n" +
                "http://www.tuniu.com/trips/12463831\n" +
                "http://www.tuniu.com/trips/12464049\n" +
                "http://www.tuniu.com/trips/12535629\n" +
                "http://www.tuniu.com/trips/12535636\n" +
                "http://www.tuniu.com/trips/12535639\n" +
                "http://www.tuniu.com/trips/12535641\n" +
                "http://www.tuniu.com/trips/12535642\n" +
                "http://www.tuniu.com/trips/12535647\n" +
                "http://www.tuniu.com/trips/12535648\n" +
                "http://www.tuniu.com/trips/12535924\n" +
                "http://www.tuniu.com/trips/10074699\n" +
                "http://www.tuniu.com/trips/10074820\n" +
                "http://www.tuniu.com/trips/10075863\n" +
                "http://www.tuniu.com/trips/10075865\n" +
                "http://www.tuniu.com/trips/10075868\n" +
                "http://www.tuniu.com/trips/10081338\n" +
                "http://www.tuniu.com/trips/10081339\n" +
                "http://www.tuniu.com/trips/10082363\n" +
                "http://www.tuniu.com/trips/10082713\n" +
                "http://www.tuniu.com/trips/10083286\n" +
                "http://www.tuniu.com/trips/10083288\n" +
                "http://www.tuniu.com/trips/10083813\n" +
                "http://www.tuniu.com/trips/10084478\n" +
                "http://www.tuniu.com/trips/10087220\n" +
                "http://www.tuniu.com/trips/10096691\n" +
                "http://www.tuniu.com/trips/10097628\n" +
                "http://www.tuniu.com/trips/10098019\n" +
                "http://www.tuniu.com/trips/10098020\n" +
                "http://www.tuniu.com/trips/10106052\n" +
                "http://www.tuniu.com/trips/12457652\n" +
                "http://www.tuniu.com/trips/12457653\n" +
                "http://www.tuniu.com/trips/12457713\n" +
                "http://www.tuniu.com/trips/12459055\n" +
                "http://www.tuniu.com/trips/12459788\n" +
                "http://www.tuniu.com/trips/12459792\n" +
                "http://www.tuniu.com/trips/12459821\n" +
                "http://www.tuniu.com/trips/12460054\n" +
                "http://www.tuniu.com/trips/10063557\n" +
                "http://www.tuniu.com/trips/10063860\n" +
                "http://www.tuniu.com/trips/10088305\n" +
                "http://www.tuniu.com/trips/10088338\n" +
                "http://www.tuniu.com/trips/10088624\n" +
                "http://www.tuniu.com/trips/10089194\n" +
                "http://www.tuniu.com/trips/10090965\n" +
                "http://www.tuniu.com/trips/10092494\n" +
                "http://www.tuniu.com/trips/10093335\n" +
                "http://www.tuniu.com/trips/10094862\n" +
                "http://www.tuniu.com/trips/10098095\n" +
                "http://www.tuniu.com/trips/10098137\n" +
                "http://www.tuniu.com/trips/10102945\n" +
                "http://www.tuniu.com/trips/10103846\n" +
                "http://www.tuniu.com/trips/10104414\n" +
                "http://www.tuniu.com/trips/10107632\n" +
                "http://www.tuniu.com/trips/10109148\n" +
                "http://www.tuniu.com/trips/10111341\n" +
                "http://www.tuniu.com/trips/10503231\n" +
                "http://www.tuniu.com/trips/10503242\n" +
                "http://www.tuniu.com/trips/12458262\n" +
                "http://www.tuniu.com/trips/10504115\n" +
                "http://www.tuniu.com/trips/10104139\n" +
                "http://www.tuniu.com/trips/10104282\n" +
                "http://www.tuniu.com/trips/10501584\n" +
                "http://www.tuniu.com/trips/10505798\n" +
                "http://www.tuniu.com/trips/12458837\n" +
                "http://www.tuniu.com/trips/12459635\n" +
                "http://www.tuniu.com/trips/12459847\n" +
                "http://www.tuniu.com/trips/12463397";
        for (String url : urls.split("\\n")) {
            System.out.println("url:'" + url + "'");
        }
    }

    public static void main(String[] args) {
        String targetUrl = "http://www.tuniu.com/trips/12462654";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(extractNoteDetail(document));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
