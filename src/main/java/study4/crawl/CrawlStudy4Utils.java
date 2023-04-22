package study4.crawl;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import study4.network.CachedNetworkOperation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrawlStudy4Utils {
    private final static CachedNetworkOperation CACHED_NETWORK_OPERATION = CachedNetworkOperation.getInstance();

    public static String fixAssetUrl(String rawUrl) {
        assert rawUrl != null;
        assert rawUrl.length() > 0;
        if (rawUrl.startsWith("https"))
            return rawUrl;
        if (rawUrl.charAt(0) == '/')
            return "https://study4.com" + rawUrl;
        return "https://study4.com/" + rawUrl;
    }

    public static String getTestSlugFromUrl(String url) {
        assert url.startsWith("https");
        String[] temp = url.split("/");
        assert temp[5].contains("-");
        return temp[5];
    }

    public static String getFullNameFromUrl(String url) {
        String slug = getTestSlugFromUrl(url);
        List<String> tokens = Arrays.stream(slug.split("-")).toList();
        tokens = tokens.stream().map(token -> {
            return Character.toUpperCase(token.charAt(0)) + token.substring(1);
        }).toList();
        return String.join(" ", tokens);
    }

    public static String getTestResultDetailUrl(String testResultUrl) {
        assert testResultUrl.startsWith("https");
        if (testResultUrl.charAt(testResultUrl.length() - 1) == '/')
            return testResultUrl + "details/";
        return testResultUrl + "/details/";
    }

    public static List<Document> getAllDocumentFromUrls(List<String> urls) throws IOException {
//        List<Document> result = new ArrayList<>();
//        for (String url : urls) {
//            result.add(Jsoup.parse(CACHED_NETWORK_OPERATION.GET(url)));
//        }
//        return result;
        return urls.parallelStream()
                .parallel()
                .map(url -> {
                    try {
                        return Jsoup.parse(CACHED_NETWORK_OPERATION.GET(url));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).toList();
    }
}
