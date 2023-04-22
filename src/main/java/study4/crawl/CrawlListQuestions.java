package study4.crawl;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import study4.crawl.part.*;
import study4.model.ToeicFullTest;
import study4.model.ToeicPart;
import study4.network.CachedNetworkOperation;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CrawlListQuestions {
    private final static Logger LOG = LoggerFactory.getLogger(CrawlListQuestions.class);
    private final static CachedNetworkOperation CACHED_NETWORK_OPERATION = CachedNetworkOperation.getInstance();

    public CrawlListQuestions() {

    }

    private String getTestResultDetailHtml(String url) throws IOException {
        return CACHED_NETWORK_OPERATION.GET(url);
    }

    private List<Element> getAllParts(Document root) {
        final Element tabContent7Parts = root.getElementsByClass("tab-content").get(0);
        final List<Element> parts = tabContent7Parts.children();
        assert parts.size() == 7;
        return parts;
    }

    private List<String> getUrlsFromResultAnswersList(Element root, final int targetNumberOfQuestions) {
        List<String> result = null;

        result = root.getElementsByTag("a")
                .stream()
                .map(a -> CrawlStudy4Utils.fixAssetUrl(a.attr("data-href")))
                .toList();

        assert result.size() == targetNumberOfQuestions;

        return result;
    }

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public ToeicFullTest crawl(String testResultUrl) throws IOException {
        //log.debug("Begin crawl test result: " + testResultUrl);
        final String testResultDetailUrl = CrawlStudy4Utils.getTestResultDetailUrl(testResultUrl);
        final Document rootResultDetail = Jsoup.parse(this.getTestResultDetailHtml(testResultDetailUrl));
        final Document rootResult = Jsoup.parse(CACHED_NETWORK_OPERATION.GET(testResultUrl));
        final ToeicFullTest toeicFullTest = new ToeicFullTest();

        toeicFullTest.setFullName(CrawlStudy4Utils.getFullNameFromUrl(testResultUrl));
        toeicFullTest.setSlug(toSlug(toeicFullTest.getFullName()));

        //log.debug("SIZE = " + rootResultDetail.toString().length() / 1024.0 + " KB");

        // Get all part
        final List<ToeicPart> toeicParts = new ArrayList<>();
        final List<Element> partElements = this.getAllParts(rootResultDetail);
        assert partElements.size() == 7;

        /// PART 1, 2, 3, 4: fetch by url
        final List<Element> resultAnswersListDiv = rootResult.getElementsByClass("result-answers-list");
        assert resultAnswersListDiv.size() == 7;

        // PART 1
        CrawlPartOne crawlPartOne = new CrawlPartOne();
        final ToeicPart partOne = crawlPartOne.crawl(this.getUrlsFromResultAnswersList(resultAnswersListDiv.get(0), 6));
        toeicParts.add(partOne);

        // PART 2
        CrawlPartTwo crawlPartTwo = new CrawlPartTwo();
        final ToeicPart partTwo = crawlPartTwo.crawl(this.getUrlsFromResultAnswersList(resultAnswersListDiv.get(1), 25));
        toeicParts.add(partTwo);

        // PART 3
        CrawlPartThree crawlPartThree = new CrawlPartThree();
        final ToeicPart partThree = crawlPartThree.crawl(this.getUrlsFromResultAnswersList(resultAnswersListDiv.get(2), 39));
        toeicParts.add(partThree);

        // PART 4
        CrawlPartFour crawlPartFour = new CrawlPartFour();
        final ToeicPart partFour = crawlPartFour.crawl(this.getUrlsFromResultAnswersList(resultAnswersListDiv.get(3), 30));
        toeicParts.add(partFour);

        // PART 5 TO 7
        CrawlPartFive crawlPartFive = new CrawlPartFive();
        final ToeicPart partFive = crawlPartFive.crawl(partElements.get(4));
        toeicParts.add(partFive);

        CrawlPartSix crawlPartSix = new CrawlPartSix();
        final ToeicPart partSix = crawlPartSix.crawl(partElements.get(5));
        toeicParts.add(partSix);

        CrawlPartSeven crawlPartSeven = new CrawlPartSeven();
        final ToeicPart partSeven = crawlPartSeven.crawl(partElements.get(6));
        toeicParts.add(partSeven);

        toeicFullTest.setParts(toeicParts);

        //log.debug("End crawl test result: " + testResultUrl);
        return toeicFullTest;
    }
}
