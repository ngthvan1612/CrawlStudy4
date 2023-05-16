package study4.crawl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import study4.model.ToeicFullTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CrawlStudy4UtilsTest {

    @org.junit.jupiter.api.Test
    void formatHtml() {
        final String html = "<div class=\"context-content context-transcript \">\n" +
                " <p><a data-toggle=\"collapse\" href=\"#transcript-f9c33aa4-82aa-4862-9286-2b203bb08396\" aria-expanded=\"false\"> Hiện Transcript <span class=\"fas fa-caret-down ml-1\"></span> </a></p>\n" +
                " <div class=\"collapse\" id=\"transcript-f9c33aa4-82aa-4862-9286-2b203bb08396\">\n" +
                "  <p>Welcome to the new employee orientation at Tockney Nature Center. We're happy that you'll be guiding our bird-watching tours! Behind me there's a trail map of the Nature Center. We'll be walking down trail two so I can show you some interesting spots to take visitors. You can use the other trails as well, except for this trail right here because there's a family of bald eagles nesting nearby. For the first time, the Center's collaborating with the state university on a research project about eagles. We've positioned a live camera on one tree there, and we don't want anything to disturb the birds for the duration of the study.</p>\n" +
                "  <p>Chào mừng tới buổi định hướng nhân viên mới tại Tockney Nature Center. Chúng tôi rất vui mừng khi bạn sẽ hướng dẫn chuyến tham quan các loài chim. Phía sau tôi là bản đồ của Nature Center. Chúng ta sẽ đi bộ xuống con đường mòn số 2 để tôi chỉ cho bạn những điểm hấp dẫn để dẫn du khách đến. Bạn có thể đi những con đường khác, ngoại trừ con đường mòn bên phải bởi vì có một đàn diều hâu gần đó. Đây là lần đầu tiên có sự hợp tác của Center với đại học bang về dự án nghiên cứu diều hâu. Chúng tôi đã lắp đặt camera trên một cái cây ở đó, và chúng ta không muốn làm xáo trộn những con chim trong suốt thời gian nghiên cứu.</p>\n" +
                " </div>\n" +
                "</div>";
        String output = CrawlStudy4Utils.formatHtml(html);
        System.out.println(output);
    }

    @org.junit.jupiter.api.Test
    void testCrawl() throws IOException {
        final CrawlListResultTests crawListResultTests = new CrawlListResultTests();
        final List<String> testResultUrls = crawListResultTests.crawl();
        final String testUrl = testResultUrls.get(testResultUrls.size() - 1);

        System.out.println("CHOOSE: " + testUrl);

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
    }
}