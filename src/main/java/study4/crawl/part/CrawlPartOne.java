package study4.crawl.part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import study4.crawl.CrawlStudy4Utils;
import study4.model.*;

import org.jsoup.nodes.Element;
import study4.network.CachedNetworkOperation;

import java.io.IOException;
import java.util.*;

public class CrawlPartOne {
    private final static Logger LOG = LoggerFactory.getLogger(CrawlPartOne.class);
    private static final CachedNetworkOperation CACHED_NETWORK_OPERATION = CachedNetworkOperation.getInstance();
    private final static Gson gsonInstance = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private class QuestionElementModel {
        private Element context;
        private Element question;
    }

    public CrawlPartOne() {

    }

    private List<ToeicAnswerChoice> parseAnswerChoices(String rawTranscript) {
        List<ToeicAnswerChoice> result = new ArrayList<>();
        String[] tokens = rawTranscript.split("\\(");

        for (String token : tokens) {
            String fixedToken = token.strip();

            if (fixedToken.toLowerCase(Locale.ROOT).contains("hiện"))
                continue;

            final String test = token.substring(0, 2);
            //LOG.debug(token);
            assert test.equals("A)") || test.equals("B)") || test.equals("C)") || test.equals("D)");

            fixedToken = fixedToken.replaceAll("[A-Z]\\)", "").strip();

            ToeicAnswerChoice choice = new ToeicAnswerChoice();
            choice.setLabel(token.strip().charAt(0) + "");
            choice.setContent("");
            choice.setExplain(fixedToken);

            //LOG.debug(choice.getLabel() + ": " + fixedToken);

            result.add(choice);
        }

        if (result.size() != 4) {
            throw new RuntimeException("Wrong number of choices [" + result.size() + "]");
        }

        return result;
    }

    private ToeicQuestionGroup parseToeicQuestionFromJsoup(Document document) {
        final Element contextWrapper = document.getElementsByClass("context-wrapper").get(0);
        final Element questionWrapper = document.getElementsByClass("question-wrapper").get(0);
        final ToeicQuestionGroup toeicQuestionGroup = new ToeicQuestionGroup();
        final ToeicQuestion toeicQuestion = new ToeicQuestion();

        toeicQuestionGroup.setQuestions(Collections.singletonList(toeicQuestion));

        // 1. Get image & audio
        final Element imgElement = contextWrapper.getElementsByTag("img").get(0);
        final Element audioElement = contextWrapper.getElementsByTag("source").get(0);

        final String fixedImageSourceUrl = CrawlStudy4Utils.fixAssetUrl(imgElement.attr("src"));
        final String fixedAudioSourceUrl = CrawlStudy4Utils.fixAssetUrl(audioElement.attr("src"));

        //LOG.debug("part 1 -> image src = " + fixedImageSourceUrl);

        ToeicItemContent mainImgContent = new ToeicItemContent();

        mainImgContent.setType("IMAGE");
        mainImgContent.setContent(fixedImageSourceUrl);

        ToeicItemContent mainAudioContent = new ToeicItemContent();
        mainAudioContent.setType("AUDIO");
        mainAudioContent.setContent(fixedAudioSourceUrl);

        toeicQuestionGroup.setQuestionContents(List.of(mainImgContent, mainAudioContent));

        // 2. Get question id
        final Element questionIdElement = questionWrapper.getElementsByTag("strong").get(0);
        toeicQuestion.setQuestionNumber(Integer.parseInt(questionIdElement.text()));

        // 3. Get 4 choices
        final String rawTranscript = contextWrapper.text();
        //LOG.debug("trans: " + rawTranscript);

        List<ToeicAnswerChoice> choices = this.parseAnswerChoices(rawTranscript);
        toeicQuestion.setChoices(choices);

        // 4. Correct answer
        final Element correctAnswerElement = questionWrapper.getElementsByClass("mt-2 text-success").get(0);
        final String correctAnswer = Arrays.stream(correctAnswerElement.text().split(":")).toList().get(1);

        if (!(correctAnswer.length() == 1 && 65 <= correctAnswer.charAt(0) && correctAnswer.charAt(0) <= 65 + 4 - 1)) {
            throw new RuntimeException("Unknown correct answer format: " + correctAnswer);
        }

        toeicQuestion.setCorrectAnswer(correctAnswer);

        return toeicQuestionGroup;
    }

    @Deprecated
    private List<QuestionElementModel> getAllQuestions(Element root) {
        final List<Element> testQuestionWrappers = root.getElementsByClass("test-questions-wrapper");
        assert testQuestionWrappers.size() == 1;

        Element testQuestionWrapper = testQuestionWrappers.get(0);

        List<QuestionElementModel> result = new ArrayList<>();

        QuestionElementModel model = null;
        for (Element element : testQuestionWrapper.getAllElements()) {
            if (element.attr("class").equals("context-wrapper")) {
                assert model == null;
                model = new QuestionElementModel();
                model.setContext(element);
            }
            else if (element.attr("class").equals("question-wrapper")) {
                assert model != null;
                model.setQuestion(element);

                result.add(model);

                model = null;
            }
        }

        assert result.size() == 6;

        return result;
    }

    public ToeicPart crawl(List<String> urls) throws IOException {
        //LOG.debug("Begin crawl part 1 ");
        ToeicPart toeicPart = new ToeicPart();
        toeicPart.setPartNumber(1);

        final List<Document> documents = CrawlStudy4Utils.getAllDocumentFromUrls(urls);
        final List<ToeicQuestionGroup> toeicQuestionGroups = new ArrayList<>();

        for (Document document : documents) {
            final ToeicQuestionGroup questionGroup = this.parseToeicQuestionFromJsoup(document);
            toeicQuestionGroups.add(questionGroup);
        }

        toeicPart.setGroups(toeicQuestionGroups);

        //LOG.info(gsonInstance.toJson(toeicPart));

        //LOG.debug("End crawl part 1 ");
        return toeicPart;
    }
}
