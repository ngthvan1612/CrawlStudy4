import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.BasicConfigurator;
import study4.crawl.CrawlListQuestions;
import study4.crawl.CrawlListResultTests;
import study4.crawl.ZipToeicFullTest;
import study4.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException {
        BasicConfigurator.configure();
        //crawList();
        circleCICrawlData();
        //crawlStream();
    }

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private static String getSlugFromString(String src) {
        String nowhitespace = WHITESPACE.matcher(src).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private static String joinQuestionGroupId(ToeicQuestionGroup group) {
        assert group.getQuestions().size() > 0;
        List<String> temp = group.getQuestions().stream().map(question -> question.getQuestionNumber().toString()).toList();
        return String.join("-", temp);
    }

    private static void crawlStream() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("tests.json");
        String json = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        List<ToeicFullTest> tests = List.of(gson.fromJson(json, ToeicFullTest[].class));

        FileWriter fileWriter = new FileWriter("list-download.txt");
        for (ToeicFullTest test : tests) {
            for (ToeicPart part : test.getParts()) {
                for (ToeicQuestionGroup group : part.getGroups()) {
                    List<ToeicItemContent> contentList = group.getQuestionContents();

                    if (contentList == null)
                        continue;

                    for (ToeicItemContent itemContent : contentList) {
                        assert itemContent.getType().equals("AUDIO") || itemContent.getType().equals("IMAGE");

                        String ext = ".mp3";
                        if (itemContent.getType().equals("IMAGE"))
                            ext = ".png";

                        final String url = itemContent.getContent();

                        final String slug = getSlugFromString(test.getFullName()) + "-" + joinQuestionGroupId(group);
                        final String fileName = slug + ext;

                        //System.out.println(fileName + "@" + url);
                        fileWriter.write(fileName + "@" + url + "\n");
                    }
                }
            }
        }
        fileWriter.close();
    }

    public static void circleCICrawlData() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("tests.json");
        String json = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
        final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        List<ToeicFullTest> tests = List.of(gson.fromJson(json, ToeicFullTest[].class));

        Map<String, String> env = System.getenv();
        final int from = Integer.parseInt(env.get("CRAWL_FROM"));
        final int to = Integer.parseInt(env.get("CRAWL_TO"));

        ZipToeicFullTest zipToeicFullTest = new ZipToeicFullTest();
        int counter = 0;
        for (ToeicFullTest test : tests) {
            if (counter < from || counter > to) {
                counter += 1;
                continue;
            }
            counter += 1;
            ByteArrayOutputStream outputStream = zipToeicFullTest.zipFile(test);
            final String fileName = "" + test.getSlug() + ".zip";
            try (OutputStream out = new FileOutputStream(fileName)) {
                outputStream.writeTo(out);
            }
        }
    }

    public static List<ToeicFullTest> crawList() throws IOException {
        final CrawlListResultTests crawListResultTests = new CrawlListResultTests();
        final List<String> testResultUrls = crawListResultTests.crawl();
        final String testUrl = testResultUrls.get(testResultUrls.size() - 1);

        log.debug("----------> Choose: " + testUrl);

        final CrawlListQuestions crawlListQuestions = new CrawlListQuestions();
        crawlListQuestions.crawl(testUrl);

        final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        List<ToeicFullTest> tests = new ArrayList<>();
        for (String x : testResultUrls) {
            final ToeicFullTest toeicFullTest = new CrawlListQuestions().crawl(x);
            tests.add(toeicFullTest);
        }

        Files.writeString(Path.of("tests.json"),
                gson.toJson(tests));

        if (1 < 2) return null;

        Map<String, String> env = System.getenv();

        ZipToeicFullTest zipToeicFullTest = new ZipToeicFullTest();
        int counter = 0;
        for (ToeicFullTest test : tests) {
            counter += 1;
            ByteArrayOutputStream outputStream = zipToeicFullTest.zipFile(test);
            final String fileName = "" + test.getSlug() + ".zip";
            try (OutputStream out = new FileOutputStream(fileName)) {
                outputStream.writeTo(out);
            }
            break;
        }

        return tests;
    }
}
