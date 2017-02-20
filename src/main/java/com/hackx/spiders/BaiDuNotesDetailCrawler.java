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
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaiDuNotesDetailCrawler extends TrspCrawlerExtractorAdapter {

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
            List<Map<String, String>> noteSection = getTravelNoteContent(document);
            if (null != noteSection && noteSection.size() > 0) {
                noteDetailList.addAll(noteSection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return noteDetailList;
    }

    public static Map<String, String> getTravelNoteAttribute(Document document) {
        Map<String, String> travelNoteAttribute = new HashMap<>();
        travelNoteAttribute.put("type", "NOTE_ATTRIBUTE");
        travelNoteAttribute.put("link", document.baseUri());

        Element basicInfoContainerEle = document.select(".basic-info-container").first();
        if (null != basicInfoContainerEle) {
            Element startTimeEle = basicInfoContainerEle.select(".start_time").first();
            if (null != startTimeEle) {
                travelNoteAttribute.put("beiginTime", startTimeEle.ownText().replace("时间：", ""));
            }
            Element duringTimeEle = basicInfoContainerEle.select(".during_time").first();
            if (null != duringTimeEle) {
                travelNoteAttribute.put("dayCount", duringTimeEle.ownText().replace("出行天数：", ""));
            }
            Element chargeInfoEle = basicInfoContainerEle.select(".charge_info").first();
            if (null != chargeInfoEle) {
                travelNoteAttribute.put("avgFee", chargeInfoEle.ownText().replace("人均费用：", ""));
            }
            Element planInfoEle = basicInfoContainerEle.select(".plan_info").first();
            if (null != planInfoEle) {
                travelNoteAttribute.put("planRoute", planInfoEle.text().replace("\uE609", ""));
            }
        }
        Element noteHeaderMainEle = document.select("div.note-header-main").first();
        if (null != noteHeaderMainEle) {
            Element notesHdEle = noteHeaderMainEle.select("div.notes-hd").first();
            if (null != notesHdEle) {
                travelNoteAttribute.put("noteTitle", notesHdEle.text());
            }
            Element userInfoEle = noteHeaderMainEle.select("p.user-info").first();
            if (userInfoEle != null) {
                travelNoteAttribute.put("autherName", userInfoEle.select("a.uname").first().text());
                Element authorImgEle = userInfoEle.getElementsByTag("img").first();
                if (authorImgEle != null) {
                    travelNoteAttribute.put("authorPic", authorImgEle.attr("src").substring(2));
                }
            }
        }
        return travelNoteAttribute;
    }

    public static List<Map<String, String>> getTravelNoteContent(Document document) {
        List<Map<String, String>> travelNotesContentSections = new ArrayList<>();
        Element masterPostsListElement = document.select("ul.master-posts-list").first();
        if (masterPostsListElement == null) return travelNotesContentSections;

        try {
            Elements postItemElements = masterPostsListElement.select(".post-item");
            for (Element postItemEle : postItemElements) {
                travelNotesContentSections.addAll(extractPostItemInfo(postItemEle));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return travelNotesContentSections;
    }

    public static List<Map<String, String>> extractPostItemInfo(Element postItemEle) {
        List<Map<String, String>> postItemInfo = new ArrayList<>();
        Element postDetailEle = postItemEle.select("div.post-detail").first();
        if (null == postDetailEle) return postItemInfo;

        try {
            /* 发布时间 */
            Element userInfoEle = postDetailEle.select("div.user-info").first();
            if (null != userInfoEle) {
                Element secondaryEle = userInfoEle.select("span.secondary").first();
                if (null != secondaryEle) {
                    postItemInfo.add(generateBlockMap(secondaryEle.text(), "PUBLISH_TIME"));
                }
            }
            /* 标题 */
            Element titleEle = postItemEle.select(".title").first();
            if (null != titleEle) {
                Map<String, String> titleMap = new HashMap<>();
                titleMap.put("content", titleEle.ownText());
                titleMap.put("type", "TITLE");
                if (null != titleEle.select("p.places").first()) {
                    titleMap.put("places", titleEle.select("p.places").first().text());
                }
                postItemInfo.add(titleMap);
            }

            Element contentEle = postItemEle.select("div.content").first();
            for (Element contentChild : contentEle.children()) {
                Elements notesPhotoImgElements = contentChild.select("img.notes-photo-img");
                if (notesPhotoImgElements.size() == 0 && !StringUtil.isBlank(contentChild.text())) { /* 无图片,有文本 */
                    Document textContDoc = Jsoup.parse(contentChild.html().replace("<br>", "\\n"));
                    postItemInfo.add(generateBlockMap(textContDoc.text(), "TEXT"));
                } else if (notesPhotoImgElements.size() == 0 && StringUtil.isBlank(contentChild.text())) { /* 无图片,无文本 */
                    continue;
                } else if (notesPhotoImgElements.size() > 0 && StringUtil.isBlank(contentChild.text())) { /* 有图片,无文本 */
                    for (Element imgEle : notesPhotoImgElements) {
                        if (StringUtil.isBlank(imgEle.attr("src"))) {
                            postItemInfo.add(generateBlockMap(imgEle.attr("data-src"), "PICTURE"));
                        } else {
                            postItemInfo.add(generateBlockMap(imgEle.attr("src"), "PICTURE"));
                        }
                    }
                } else if (notesPhotoImgElements.size() > 0 && !StringUtil.isBlank(contentChild.text())) { /* 有图片,有文本 */
                    for (int i = 0; i < contentChild.childNodeSize(); i++) {
                        if ("#text".equals(contentChild.childNode(i).nodeName())) {
                            postItemInfo.add(generateBlockMap(contentChild.childNode(i).outerHtml(), "TEXT"));
                            continue;
                        }
                        Element firstLevelChildEle = (Element) contentChild.childNode(i);
                        for (int j = 0; j < firstLevelChildEle.childNodeSize(); j++) {
                            if ("#text".equals(firstLevelChildEle.childNode(j).nodeName())) {
                                postItemInfo.add(generateBlockMap(firstLevelChildEle.childNode(j).outerHtml(), "TEXT"));
                                continue;
                            }
                            Element secondLevelChildEle = (Element) firstLevelChildEle.childNode(j);
                            postItemInfo.addAll(extractMixedElement(secondLevelChildEle));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return postItemInfo;
    }

    public static List<Map<String, String>> extractMixedElement(Element rootElement) {
        List<Map<String, String>> list = new ArrayList<>();
        Element notesPhotoImgEle = rootElement.select("img.notes-photo-img").first();
        if (null == notesPhotoImgEle) { /* 纯文本 */
            if (!StringUtil.isBlank(rootElement.text())) {
                Document textContentDoc = Jsoup.parse(rootElement.html().replace("<br>", "\n"));
                list.add(generateBlockMap(textContentDoc.text(), "TEXT"));
            }
        } else {
            if (StringUtil.isBlank(rootElement.text())) { /* 纯图片 */
                for (Element picEle : rootElement.select("img.notes-photo-img")) {
                    if (StringUtil.isBlank(picEle.attr("src"))) {
                        list.add(generateBlockMap(picEle.attr("data-src"), "PICTURE"));
                    } else {
                        list.add(generateBlockMap(picEle.attr("src"), "PICTURE"));
                    }
                }
            } else { /* 图片和文本混合 */
                List<Node> childNodes = rootElement.childNodes();
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
                            list.add(generateBlockMap(textBlock, "TEXT"));
                            textBlock = "";
                        }
                        if (StringUtil.isBlank(imgEle.attr("src"))) {
                            list.add(generateBlockMap(imgEle.attr("data-src"), "PICTURE"));
                        } else {
                            list.add(generateBlockMap(imgEle.attr("src"), "PICTURE"));
                        }
                    } else {
                        textBlock = textBlock + childEle.text();
                    }
                }
                if (!StringUtil.isBlank(textBlock)) {
                    list.add(generateBlockMap(textBlock, "TEXT"));
                }
            }
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
        String targetUrl = "https://lvyou.baidu.com/notes/d64db92717bab9d9f907574d-1";
        generateUrls();
        /*try {
            Document document = Jsoup.connect(targetUrl).get();
            System.out.println(extractNoteDetail(document));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public static void generateUrls() {
        String urls = "https://lvyou.baidu.com/notes/d64db92717bab9d9f907574d\n" +
                "https://lvyou.baidu.com/notes/8a634c347fbd61e899e2d34c\n" +
                "https://lvyou.baidu.com/notes/c50d553d1354b88b3b405e48\n" +
                "https://lvyou.baidu.com/notes/5d082876db1d304e2cd222af\n" +
                "https://lvyou.baidu.com/notes/36751b74a5642591f2c70e1c\n" +
                "https://lvyou.baidu.com/notes/691371284c37e41d8dba6134\n" +
                "https://lvyou.baidu.com/notes/032f26a62032f6dc5f37c292\n" +
                "https://lvyou.baidu.com/notes/0c36cb3a4008cfd815740de9\n" +
                "https://lvyou.baidu.com/notes/5c00c71030743d04419ddffe\n" +
                "https://lvyou.baidu.com/notes/166227d981d5f63521fcb1fe\n" +
                "https://lvyou.baidu.com/notes/6d6c33dd8df75a3d13545c80\n" +
                "https://lvyou.baidu.com/notes/bb8eec124ea85aff5c19bbfe\n" +
                "https://lvyou.baidu.com/notes/ba769f4cc41030743d04dcdc\n" +
                "https://lvyou.baidu.com/notes/fcae53193721a32e24d9bce3\n" +
                "https://lvyou.baidu.com/notes/aa47f54409b289e2b755f74f\n" +
                "https://lvyou.baidu.com/notes/82ceaf048b25ce71afa5857a\n" +
                "https://lvyou.baidu.com/notes/9d52cc5e4a3365b88b2d0414\n" +
                "https://lvyou.baidu.com/notes/779b8736a9574115277621bb\n" +
                "https://lvyou.baidu.com/notes/75efc66fb3ad38026f922db4\n" +
                "https://lvyou.baidu.com/notes/d60eb92717bab9d9f907570e\n" +
                "https://lvyou.baidu.com/notes/79dbda0fe617700dc81f9112\n" +
                "https://lvyou.baidu.com/notes/3743ed34c70f21a62032c0ee\n" +
                "https://lvyou.baidu.com/notes/3f1cc3da9f42fb3bf11afb6e\n" +
                "https://lvyou.baidu.com/notes/990ff1fc14c2ef124ea8b91f\n" +
                "https://lvyou.baidu.com/notes/84a08d2bca0140b47b71a3c0\n" +
                "https://lvyou.baidu.com/notes/b17fbfa5b6e23f5d281486bc\n" +
                "https://lvyou.baidu.com/notes/87701e79d8e75529bc3e1049\n" +
                "https://lvyou.baidu.com/notes/03bc06b289e2b755a75af64a\n" +
                "https://lvyou.baidu.com/notes/5115b5c1fa0b9e277726aab9\n" +
                "https://lvyou.baidu.com/notes/0a30e683c6d80ff538fab446\n" +
                "https://lvyou.baidu.com/notes/af9dd236d9e590e08d8b1c06\n" +
                "https://lvyou.baidu.com/notes/29aad4c0cdda9f42fb3bf804\n" +
                "https://lvyou.baidu.com/notes/8e2d8ed5f63521fca4c4b046\n" +
                "https://lvyou.baidu.com/notes/2f5bd08513ff2d63acd47419\n" +
                "https://lvyou.baidu.com/notes/fbb23821a32e24d981d5bf19\n" +
                "https://lvyou.baidu.com/notes/0fde53d4cb1cdbc0cddae688\n" +
                "https://lvyou.baidu.com/notes/5d581bc2ef124ea85affb816\n" +
                "https://lvyou.baidu.com/notes/d07a9ee08d8b1179d8e71e2d\n" +
                "https://lvyou.baidu.com/notes/d17e9fe08d8b1179d8e71e29\n" +
                "https://lvyou.baidu.com/notes/d5a9d7e590e08d8b11791f2d\n" +
                "https://lvyou.baidu.com/notes/2da9d0c0cdda9f42fb3bf807\n" +
                "https://lvyou.baidu.com/notes/94c1bc3452374ac89ab433c7\n" +
                "https://lvyou.baidu.com/notes/78cd210992ad52baf344f2c3\n" +
                "https://lvyou.baidu.com/notes/2885d5c0cdda9f42fb3bf82b\n" +
                "https://lvyou.baidu.com/notes/7cee44c89ab4e6e6a5f7356b\n" +
                "https://lvyou.baidu.com/notes/f7134e9d43347fbd61e8d0a5\n" +
                "https://lvyou.baidu.com/notes/e5a46ee9d66fb3ad38022a2e\n" +
                "https://lvyou.baidu.com/notes/e5a46ee9d66fb3ad38022a2e\n" +
                "https://lvyou.baidu.com/notes/bc8c175ffca2b8d5326ecbef\n" +
                "https://lvyou.baidu.com/notes/3a8dce1ea5c1fa0b9e27abc3\n" +
                "https://lvyou.baidu.com/notes/90cacd36aec20606a7048072\n" +
                "https://lvyou.baidu.com/notes/493e54ffebdb20f36a5ad76e\n" +
                "https://lvyou.baidu.com/notes/cd359bbc2d0992ad52baf3c3\n" +
                "https://lvyou.baidu.com/notes/2ea821d671563fe1e8927d74\n" +
                "https://lvyou.baidu.com/notes/573a49f0df8513ff2d63757b\n" +
                "https://lvyou.baidu.com/notes/2cfd0f06a7048b25ce71828d\n" +
                "https://lvyou.baidu.com/notes/49ef43a85aff5c193721ba4f\n" +
                "https://lvyou.baidu.com/notes/bab9db1cdbc0cdda9f42f9df\n" +
                "https://lvyou.baidu.com/notes/d19d9fe08d8b1179d8e71eca\n" +
                "https://lvyou.baidu.com/notes/5c11a3d4a9d00c11e3de89c0\n" +
                "https://lvyou.baidu.com/notes/9c092f32f6dc5f373c93c51d\n" +
                "https://lvyou.baidu.com/notes/df17a7d00c11e3de71f38871\n" +
                "https://lvyou.baidu.com/notes/7058b4cae00799b2b62752aa\n" +
                "https://lvyou.baidu.com/notes/0df1802a17f3f701b2dd4ba1\n" +
                "https://lvyou.baidu.com/notes/5fde2a91f2c7e43162f70009\n" +
                "https://lvyou.baidu.com/notes/94381be73fe2902a17f3497d\n" +
                "https://lvyou.baidu.com/notes/b28141f59fdd26225405140d\n" +
                "https://lvyou.baidu.com/notes/b442c91cdbc0cdda9f42f924\n" +
                "https://lvyou.baidu.com/notes/7afbdb0fe617700dc81f9132\n" +
                "https://lvyou.baidu.com/notes/c883b9b4b03452374ac830d9\n" +
                "https://lvyou.baidu.com/notes/19e07a29a929dd36d9e51a1f\n" +
                "https://lvyou.baidu.com/notes/3a940ad2812bca0140b4a0da\n" +
                "https://lvyou.baidu.com/notes/68875526d608160706c2431d\n" +
                "https://lvyou.baidu.com/notes/48564f373c93c7305dfac738\n" +
                "https://lvyou.baidu.com/notes/4e38e434c70f21a62032c095\n" +
                "https://lvyou.baidu.com/notes/0db2bd381f3df627eb0a8cf3\n" +
                "https://lvyou.baidu.com/notes/82ce3bd883f5c6c123f371ee\n" +
                "https://lvyou.baidu.com/notes/069f0fff2850e9e45ad4e5c4\n" +
                "https://lvyou.baidu.com/notes/32fbd58513ff2d63acd474b9\n" +
                "https://lvyou.baidu.com/notes/ba858b2bca0140b47b71a3e5\n" +
                "https://lvyou.baidu.com/notes/5631aac1fa0b9e277726aa9d\n" +
                "https://lvyou.baidu.com/notes/839a410358504ad767aa39ce\n" +
                "https://lvyou.baidu.com/notes/a47c60e899e25affebdbd573\n" +
                "https://lvyou.baidu.com/notes/9a83a5c4e983c6d80ff5b5cd\n" +
                "https://lvyou.baidu.com/notes/b1941c5ffca2b8d5326ecbf7\n" +
                "https://lvyou.baidu.com/notes/38613c5d2814ea6d9fc99831\n" +
                "https://lvyou.baidu.com/notes/e6e66fdb78a0fee8bab43fc5\n" +
                "https://lvyou.baidu.com/notes/8be853347fbd61e899e2d3c7\n" +
                "https://lvyou.baidu.com/notes/4db961563fe1e89235d87cdd\n" +
                "https://lvyou.baidu.com/notes/a42ffc1d234497bc2d09f129\n" +
                "https://lvyou.baidu.com/notes/d79099e08d8b1179d8e71ec7\n" +
                "https://lvyou.baidu.com/notes/000e05ff2850e9e45ad4e555\n" +
                "https://lvyou.baidu.com/notes/47bb250cf13861e9d66f280c\n" +
                "https://lvyou.baidu.com/notes/1a24d7e75529bc3e4ef513ef\n" +
                "https://lvyou.baidu.com/notes/3c4c5a3365b88b2d545807a1\n" +
                "https://lvyou.baidu.com/notes/3c4c5a3365b88b2d545807a1\n" +
                "https://lvyou.baidu.com/notes/3c4c5a3365b88b2d545807a1\n" +
                "https://lvyou.baidu.com/notes/4327dc16cc9163db78a03d3f\n" +
                "https://lvyou.baidu.com/notes/38a14c2591f008f7749047d1\n" +
                "https://lvyou.baidu.com/notes/eb7d190706c25c2591f045c7\n" +
                "https://lvyou.baidu.com/notes/9c505d374ac89ab4e6e632d6\n" +
                "https://lvyou.baidu.com/notes/202660929736a95741152e96\n" +
                "https://lvyou.baidu.com/notes/c428543d1354b88b3b405e6d\n" +
                "https://lvyou.baidu.com/notes/29b608c25c2591f008f74403\n" +
                "https://lvyou.baidu.com/notes/4dca230cf13861e9d66f287d\n" +
                "https://lvyou.baidu.com/notes/a42b4b5c456e28d671567b3e\n" +
                "https://lvyou.baidu.com/notes/93443e026f929736a9572f5b\n" +
                "https://lvyou.baidu.com/notes/7575ee0ac336aec2060681e0\n" +
                "https://lvyou.baidu.com/notes/aba524fd6ed314246e516b10\n" +
                "https://lvyou.baidu.com/notes/9e61703c0b597f188f4cdb29\n" +
                "https://lvyou.baidu.com/notes/5306fc6cce3a4008cfd80aef\n" +
                "https://lvyou.baidu.com/notes/70fb72743432cda7425c6677\n" +
                "https://lvyou.baidu.com/notes/68af882d5458084d782919a5\n" +
                "https://lvyou.baidu.com/notes/64ad55baf34409b289e2f4b2\n" +
                "https://lvyou.baidu.com/notes/14fad708160706c25c25426e\n" +
                "https://lvyou.baidu.com/notes/586440f0df8513ff2d637525\n" +
                "https://lvyou.baidu.com/notes/c749b62717bab9d9f9075749\n" +
                "https://lvyou.baidu.com/notes/44de64b88b2d5458084d065f\n" +
                "https://lvyou.baidu.com/notes/2d45a9d00c11e3de71f38823\n" +
                "https://lvyou.baidu.com/notes/bd5023f341f0df8513ff7223\n" +
                "https://lvyou.baidu.com/notes/ef7c5c193721a32e24d9bc31\n" +
                "https://lvyou.baidu.com/notes/4860e89235d883f5c6c17e33\n" +
                "https://lvyou.baidu.com/notes/5cce93f5c6c123f341f070a4\n" +
                "https://lvyou.baidu.com/notes/be0e37fa0fd1185ffca2c949\n" +
                "https://lvyou.baidu.com/notes/d1459fe08d8b1179d8e71e12\n" +
                "https://lvyou.baidu.com/notes/19556df7c25e4a3365b805d7\n" +
                "https://lvyou.baidu.com/notes/b84282ba65e9f1fa163e62ed\n" +
                "https://lvyou.baidu.com/notes/e7032150e9e45ad4cb1ce44e\n" +
                "https://lvyou.baidu.com/notes/122320d2ace64003585027df\n" +
                "https://lvyou.baidu.com/notes/f2c3f52d78c54d8857284fac\n" +
                "https://lvyou.baidu.com/notes/bf9ba0c20606a7048b25831f\n" +
                "https://lvyou.baidu.com/notes/40637d902aef233c3f5e5826\n" +
                "https://lvyou.baidu.com/notes/9f8012e73fe2902a17f349c5\n" +
                "https://lvyou.baidu.com/notes/83a75d58084d7829a9291838\n" +
                "https://lvyou.baidu.com/notes/1cb84a104db4f31d2344ff10\n" +
                "https://lvyou.baidu.com/notes/46a2b53e4ef59fdd26221539\n" +
                "https://lvyou.baidu.com/notes/09ddf43162f7c25e4a3302a9\n" +
                "https://lvyou.baidu.com/notes/7bfba8048b25ce71afa5854f\n" +
                "https://lvyou.baidu.com/notes/59452dfca4c4e983c6d8b2c2\n" +
                "https://lvyou.baidu.com/notes/9fbaac2e24d981d5f635be29\n" +
                "https://lvyou.baidu.com/notes/366f51373c93c7305dfac701\n" +
                "https://lvyou.baidu.com/notes/e406cf1fabb7e03a496792b9\n" +
                "https://lvyou.baidu.com/notes/34efc81ea5c1fa0b9e27aba1\n" +
                "https://lvyou.baidu.com/notes/0636dbe75529bc3e4ef513fd\n" +
                "https://lvyou.baidu.com/notes/89522f2254052b0cf138163d\n" +
                "https://lvyou.baidu.com/notes/61a1ff27eb0ac336aec28e2e\n" +
                "https://lvyou.baidu.com/notes/6f0e47b47b713c0a3ba2a5bd\n" +
                "https://lvyou.baidu.com/notes/cefd1374a5642591f2c70e97\n" +
                "https://lvyou.baidu.com/notes/44ad71902aef233c3f5e58e8\n" +
                "https://lvyou.baidu.com/notes/f5bde03162f7c25e4a3302c8\n" +
                "https://lvyou.baidu.com/notes/cb551674a5642591f2c70e3f\n" +
                "https://lvyou.baidu.com/notes/b852a1c20606a7048b2583d6\n" +
                "https://lvyou.baidu.com/notes/d36806f774902aef233c592a\n" +
                "https://lvyou.baidu.com/notes/5ad289f5c6c123f341f070b8\n" +
                "https://lvyou.baidu.com/notes/76519df75a3d1354b88b5f3e\n" +
                "https://lvyou.baidu.com/notes/302eff3521fca4c4e983b349";
        for (String url : urls.split("\n")) {
            System.out.println("id:'" + url.replace("https://lvyou.baidu.com/notes/", "") + "'");
        }
    }
}
