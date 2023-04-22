package study4.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import study4.network.CachedNetworkOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawlListResultTests {
    private static final CachedNetworkOperation CACHED_NETWORK_OPERATION = CachedNetworkOperation.getInstance();
    private static final Logger LOG = LoggerFactory.getLogger(CrawlListResultTests.class);

    public CrawlListResultTests() {

    }

    private Document getListTestsByPageIndex(int pageId) throws IOException {
        return Jsoup.parse(CACHED_NETWORK_OPERATION.GET("https://study4.com/my-account/tests/?page=" + pageId));
    }

    private List<String> getListResultUrlFromOnePage(Document root) {
        List<Element> trs = root.getElementsByTag("tr")
                .stream()
                .filter(tr -> tr.getElementsByTag("td").size() == 4)
                .toList();

        List<String> hrefs = new ArrayList<>();

        for (Element element : trs) {
            String href = element.getElementsByTag("td").last()
                    .getElementsByTag("a").get(0).attr("href");

            hrefs.add(href);
        }

        return hrefs;
    }

    private List<String> filterETSNewFormatToeic(List<String> urls) {
        return urls.stream().filter(url -> {
            if (!url.contains("ets"))
                return false;
            return !url.contains("old");
        }).toList();
    }

    private void debugListUrls(List<String> urls) {
        for (String url : urls) {
            final String slug = CrawlStudy4Utils.getTestSlugFromUrl(url);
            //log.debug("url = " + url + ", slug = " + slug);
        }
    }

    public List<String> crawl() throws IOException {
        List<Document> documents = new ArrayList<>();

        for (int i = 1; i <= 13; ++i) {
            documents.add(this.getListTestsByPageIndex(i));
        }

        List<String> rawUrls = new ArrayList<>();
        for (Document document : documents) {
            final List<String> rawListUrls = this.getListResultUrlFromOnePage(document);

            List<String> fixedListUrls = rawListUrls
                    .stream().map(CrawlStudy4Utils::fixAssetUrl).toList();

            fixedListUrls = this.filterETSNewFormatToeic(fixedListUrls);

            rawUrls.addAll(fixedListUrls);
        }

        Map<String, String> map = new HashMap<>();

        for (String url : rawUrls) {
            final String slug = CrawlStudy4Utils.getTestSlugFromUrl(url);
            map.put(slug, url);
        }

        List<String> urls = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            urls.add(entry.getValue());
        }

        this.debugListUrls(urls);

        return urls;
    }
}
