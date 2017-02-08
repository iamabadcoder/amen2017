package com.hackx.travelnotes;

import com.google.gson.Gson;
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

public class BaiDuTravelNotesExtraction {

    public static void main(String[] args) {

        String targetUrl = "https://lvyou.baidu.com/notes/bd52085ffca2b8d5326ecb31";
        try {
            Document document = Jsoup.connect(targetUrl).get();
            /*System.out.println(document.html());*/
            Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);
            /*System.out.println(travelNotesInfoAttribute);*/
            List<List<Map<String, String>>> travelNotesContentSections = getTravelNotesContentSections(document, targetUrl);
            System.out.println(new Gson().toJson(travelNotesContentSections));
        } catch (Exception e) {
            System.out.println("Exception when connect to targetUrl:" + targetUrl + e.getMessage());
        }
    }

    public static List<List<Map<String, String>>> getTravelNotesContentSections(Document document, String targetUrl) throws Exception {
        Map<Integer, String> catalogMap = getCatalogInfo(document);
        List<List<Map<String, String>>> travelNotesContentSections = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : catalogMap.entrySet()) {
            Element postItemEle = document.getElementById("F" + entry.getKey());
            if (null != postItemEle) {
                travelNotesContentSections.add(extractPostItemInfo(postItemEle));
            } else {
                document = Jsoup.connect(targetUrl + "-" + entry.getKey()).get();
                postItemEle = document.getElementById("F" + entry.getKey());
                if (null != postItemEle) {
                    travelNotesContentSections.add(extractPostItemInfo(postItemEle));
                }
            }
        }
        return travelNotesContentSections;
    }

    public static List<Map<String, String>> extractPostItemInfo(Element postItemEle) {
        List<Map<String, String>> postItemInfo = new ArrayList<>();
        Element secondaryEle = postItemEle.select("span.secondary").first();
        if (null != secondaryEle) {
            postItemInfo.add(generateBlockMap(secondaryEle.text(), "PUBLISH_TIME"));
        }

        Element titleEle = postItemEle.select(".title").first();
        if (null != titleEle) {
            postItemInfo.add(generateBlockMap(titleEle.text(), "TITLE"));
        }

        Element contentEle = postItemEle.select("div.content").first();
        Element currChildEle = contentEle.child(0);
        while (null != currChildEle) {
            Element notesPhotoImgEle = currChildEle.select("img.notes-photo-img").first();
            if (null == notesPhotoImgEle) { /* 纯文本 */
                Document textContentDoc = Jsoup.parse(currChildEle.html().replaceAll("<br>", "\\\\n"));
                postItemInfo.add(generateBlockMap(textContentDoc.text(), "TEXT"));
            } else {
                if (StringUtil.isBlank(currChildEle.text())) { /* 纯图片 */
                    postItemInfo.add(generateBlockMap(notesPhotoImgEle.attr("src"), "PICTURE"));
                } else { /* 图片和文本混合 */
                    List<Node> childNodes = currChildEle.childNodes();
                    String textBlock = "";
                    for (Node node : childNodes) {
                        if ("#text".equals(node.nodeName())) {
                            textBlock = textBlock + node.outerHtml();
                            continue;
                        }
                        Element childEle = (Element) node;
                        Element imgEle = childEle.select("img.notes-photo-img").first();
                        if (null != imgEle) {
                            if (!StringUtil.isBlank(textBlock)) {
                                postItemInfo.add(generateBlockMap(textBlock, "TEXT"));
                                textBlock = "";
                            }
                            postItemInfo.add(generateBlockMap(imgEle.attr("src"), "PICTURE"));
                        } else {
                            textBlock = textBlock + childEle.text();
                        }
                    }
                    if (!StringUtil.isBlank(textBlock)) {
                        postItemInfo.add(generateBlockMap(textBlock, "TEXT"));
                    }
                }
            }
            currChildEle = currChildEle.nextElementSibling();
        }
        return postItemInfo;
    }

    public static Map<String, String> generateBlockMap(String content, String type) {
        return new HashMap<String, String>() {
            {
                put("content", content);
                put("type", type);
            }
        };
    }

    public static Map<Integer, String> getCatalogInfo(Document document) {
        Map<Integer, String> catalogMap = new HashMap<>();
        Element catalogListEle = document.select("ul.catalog-list").first();
        if (null != catalogListEle) {
            Elements catalogElements = catalogListEle.select("a[data-hash^=#F]");
            System.out.println(catalogElements.size());
            for (Element catalogEle : catalogElements) {
                catalogMap.put(Integer.valueOf(catalogEle.attr("data-hash").replace("#F", "")), catalogEle.ownText());
            }
        }
        return catalogMap;
    }


    public static Map<String, String> getTravelNotesInfoAttribute(Document document) {
        Map<String, String> travelNotesInfoAttribute = new HashMap<>();
        Element basicInfoContainerEle = document.select(".basic-info-container").first();
        if (null != basicInfoContainerEle) {
            Element startTimeEle = basicInfoContainerEle.select(".start_time").first();
            if (null != startTimeEle) {
                travelNotesInfoAttribute.put("startTime", startTimeEle.ownText().replace("时间：", ""));
            }
            Element duringTimeEle = basicInfoContainerEle.select(".during_time").first();
            if (null != duringTimeEle) {
                travelNotesInfoAttribute.put("duringTime", duringTimeEle.ownText().replace("出行天数：", ""));
            }
            Element chargeInfoEle = basicInfoContainerEle.select(".charge_info").first();
            if (null != chargeInfoEle) {
                travelNotesInfoAttribute.put("chargeInfo", chargeInfoEle.ownText().replace("人均费用：", ""));
            }
            Element planInfoEle = basicInfoContainerEle.select(".plan_info").first();
            if (null != planInfoEle) {
                travelNotesInfoAttribute.put("planInfo", planInfoEle.text().replace("\uE609", ""));
            }
        }
        Element noteHeaderMainEle = document.select("div.note-header-main").first();
        if (null != noteHeaderMainEle) {
            Element notesHdEle = noteHeaderMainEle.select("div.notes-hd").first();
            if (null != notesHdEle) {
                travelNotesInfoAttribute.put("noteTitle", notesHdEle.text());
            }
            Element userInfoEle = noteHeaderMainEle.select("p.user-info").first();
            if (userInfoEle != null) {
                travelNotesInfoAttribute.put("autherName", userInfoEle.select("a.uname").first().text());
                Element authorImgEle = userInfoEle.getElementsByTag("img").first();
                if (authorImgEle != null) {
                    travelNotesInfoAttribute.put("authorImg", authorImgEle.attr("src").substring(2));
                }
            }
        }
        return travelNotesInfoAttribute;
    }

}
