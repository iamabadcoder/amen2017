package com.hackx.fliggy.spiders;

/**
 * Created by hackx on 2/27/17.
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.trip.tripspider.downloader.domain.exception.TrspDownloadException;
import com.alibaba.trip.tripspider.extractor.TrspExtractUtils;
import com.alibaba.trip.tripspider.extractor.domain.TrspExtractResultDO;
import com.alibaba.trip.tripspider.extractor.domain.exception.TrspExtractException;
import com.alibaba.trip.tripspider.httpclient.domain.TrspHttpRequestParam;
import com.alibaba.trip.tripspider.httpclient.domain.enumerate.TrspHttpMethod;
import com.alibaba.trip.tripspider.spider.crawler.TrspCrawlerAdapter;
import com.alibaba.trip.tripspider.spider.domain.TrspSpiderJobParamDO;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by vento on 2017/2/15.
 */
public class QyerCityUrlsCrawler extends TrspCrawlerAdapter {

    Map<String, String> header;

    public String Get(String url, Map<String, String> param) {
        TrspHttpRequestParam httpRequestParam = new TrspHttpRequestParam();
        httpRequestParam.setUrl(url);
        httpRequestParam.setParams(param);
        httpRequestParam.setHeaders(header);
        httpRequestParam.setHttpMethod(TrspHttpMethod.GET);
        return trspHttpManager.request(httpRequestParam).getBody();
    }


    public JSONArray crawl_continent(String url, Map<String, String> param, JSONObject envParam) {
        JSONArray result = new JSONArray();
        Document document = TrspExtractUtils.toDocument(this.Get(url, param));
        Elements elements = document.select("nav.plcIndexNav").select("a[href]");
        for (Element element : elements) {
            JSONObject jo = (JSONObject) envParam.clone();
            jo.put("continent", element.text());
            jo.put("continent_url", "http:" + element.attr("href"));
            result.add(jo);
        }
        return result;
    }

    public JSONArray crawl_country(String url, Map<String, String> param, JSONObject envParam) {
        JSONArray result = new JSONArray();
        Document document = TrspExtractUtils.toDocument(this.Get(url, param));
        Elements elements = document.select("article.plcAllCountrys").select("a[href]");
        for (Element element : elements) {
            JSONObject jo = (JSONObject) envParam.clone();
            jo.put("country", element.ownText());
            jo.put("country_en", element.select("span.en").first().ownText());
            jo.put("country_url", "http:" + element.attr("href"));
            result.add(jo);
        }
        return result;
    }


    public JSONArray crawl_city(String url, Map<String, String> param, JSONObject envParam) {
        JSONArray result = new JSONArray();
        Document document = TrspExtractUtils.toDocument(this.Get(url, param));
        Elements allCityLink = document.select("article.city-list-box").select("a.btn-more");
        Elements poiListBox = document.select("article.poi-list-box");
        Elements cityListBox = document.select("article.city-list-box");
        if (allCityLink.isEmpty()) {
            if (!poiListBox.isEmpty()) {
                /* isCity */
                JSONObject jo = (JSONObject) envParam.clone();
                jo.put("city", envParam.getString("country"));
                jo.put("city_en", envParam.getString("country_en"));
                jo.put("city_url", envParam.getString("country_url"));
                result.add(jo);
            }
        } else {
                /* handle btn-more */
            Element cityAllElement = allCityLink.first();
            Document cityAllDocument = TrspExtractUtils.toDocument(this.Get("http:" + cityAllElement.attr("href"), new HashMap<String, String>()));
            Elements cityListElements = cityAllDocument.select("article.plcCityList").select("a[href]");
            for (Element element : cityListElements) {
                JSONObject jo = (JSONObject) envParam.clone();
                jo.put("city", element.ownText());
                jo.put("city_en", element.select("span.en").first().ownText());
                jo.put("city_url", "http:" + element.attr("href"));
                result.add(jo);
            }
        }
        return result;
    }


    public TrspExtractResultDO crawl(TrspSpiderJobParamDO p) throws TrspExtractException, TrspDownloadException, Exception {

        header = new HashMap<String, String>();
        header.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1");

        TrspExtractResultDO trspExtractResultDO = new TrspExtractResultDO();
        JSONObject crawlParam = p.getParam();
        JSONArray result = null;


        JSONArray continentUrls = this.crawl_continent(crawlParam.getString("base"), new HashMap<String, String>(), new JSONObject());


        JSONArray countryUrls = new JSONArray();
        for (Iterator iter = continentUrls.iterator(); iter.hasNext(); ) {
            JSONObject continentUrl = (JSONObject) iter.next();
            JSONArray tmp = this.crawl_country(continentUrl.getString("continent_url"), new HashMap<String, String>(), continentUrl);
            for (Iterator iter1 = tmp.iterator(); iter1.hasNext(); )
                countryUrls.add(iter1.next());
        }

        JSONArray cityUrls = new JSONArray();
        for (Iterator iter = countryUrls.iterator(); iter.hasNext(); ) {
            JSONObject countryUrl = (JSONObject) iter.next();
            JSONArray tmp = this.crawl_city(countryUrl.getString("country_url"), new HashMap<String, String>(), countryUrl);
            for (Iterator iter1 = tmp.iterator(); iter1.hasNext(); )
                cityUrls.add(iter1.next());
        }


        result = cityUrls;
        trspExtractResultDO.setResult(result);
        return trspExtractResultDO;
    }


}

