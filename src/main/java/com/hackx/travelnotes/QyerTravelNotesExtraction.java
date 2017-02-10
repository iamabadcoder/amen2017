package com.hackx.travelnotes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hackx on 2/9/17.
 */
public class QyerTravelNotesExtraction {
    public static Integer MAX_PAGE = 20;

    public static void main(String[] args) {

        String startUrl = "http://bbs.qyer.com/thread-2663094-1.html";
        String pageUrl = "http://bbs.qyer.com/thread-2663094-1.html?authorid=352835&page=1";

        try {
            Document document = Jsoup.connect(startUrl).get();
            /*System.out.println(document.html());*/
            Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);
            System.out.println(travelNotesInfoAttribute);
            Element authoronlyEle = document.select("a[data-bn-ipg*=bbs-thread-top-authoronly]").first();
            for (int i = 1; i <= MAX_PAGE; i++) {
                String targetUrl = "http:" + authoronlyEle.attr("href") + "&page=" + i;



            }


            /*Map<String, String> travelNotesInfoAttribute = getTravelNotesInfoAttribute(document);*/
            /*System.out.println(travelNotesInfoAttribute);*/
            /*List<List<Map<String, String>>> travelNotesContentSections = getTravelNotesContentSections(document, targetUrl);
            System.out.println(new Gson().toJson(travelNotesContentSections));*/
        } catch (Exception e) {
            System.out.println("Exception when connect to targetUrl:" + startUrl + e);
        }


    }

    public static Map<String, String> getTravelNotesInfoAttribute(Document document) {
        Map<String, String> travelNotesInfoAttribute = new HashMap<>();

        Element titleEle = document.select("h3.b_tle").first();
        if (null != titleEle) {
            travelNotesInfoAttribute.put("notesTitle", titleEle.text());
        }
        Element userNameEle = document.select("a[data-bn-ipg=bbs-thread-top-username]").first();
        if (null != userNameEle) {
            travelNotesInfoAttribute.put("userName", userNameEle.text());
        }
        Element userPhotoEle = document.select("a[data-bn-ipg=bbs-thread-top-userphoto]").first();
        if (null != userPhotoEle && null != userPhotoEle.select("img").first()) {
            travelNotesInfoAttribute.put("userPhoto", userPhotoEle.select("img").first().attr("src"));
        }
        return travelNotesInfoAttribute;
    }
}
