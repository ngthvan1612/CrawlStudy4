package study4.crawl.part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jsoup.nodes.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import study4.crawl.CrawlStudy4Utils;
import study4.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CrawlPartTwo {
    private final static Logger LOG = LoggerFactory.getLogger(CrawlPartTwo.class);
    private final static Gson gsonInstance = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public CrawlPartTwo() {

    }

    private String getQuestionRawTranscript(String rawTranscript) {
        List<ToeicAnswerChoice> result = new ArrayList<>();
        String[] tokens = rawTranscript.split("\\(");

        for (String token : tokens) {
            String fixedToken = token.strip();

            if (fixedToken.toLowerCase(Locale.ROOT).contains("hiện")) {
                assert fixedToken.contains("Hiện Transcript");
                assert fixedToken.contains("Your browser does not support the audio element.");
                fixedToken = fixedToken.replace("Your browser does not support the audio element.", "");
                fixedToken = fixedToken.replace("Hiện Transcript", "").strip();
                return fixedToken;
            }
        }

        throw new RuntimeException("Cannot found question from " + rawTranscript);
    }
    
    private List<ToeicAnswerChoice> parseAnswerChoices(String rawTranscript) {
        List<ToeicAnswerChoice> result = new ArrayList<>();
        String[] tokens = rawTranscript.split("\\(");

        for (String token : tokens) {
            String fixedToken = token.strip();

            if (fixedToken.toLowerCase(Locale.ROOT).contains("hiện"))
                continue;

            final String test = token.substring(0, 2);
            assert test.equals("A)") || test.equals("B)") || test.equals("C)") || test.equals("D)");

            fixedToken = fixedToken.replaceAll("[A-Z]\\)", "").strip();

            ToeicAnswerChoice choice = new ToeicAnswerChoice();
            choice.setLabel(token.strip().charAt(0) + "");
            choice.setExplain(fixedToken);
            choice.setContent("");

            //log.debug(choice.getLabel() + ": " + fixedToken);

            result.add(choice);
        }

        if (result.size() != 3) {
            throw new RuntimeException("Wrong number of choices [" + result.size() + "]");
        }

        return result;
    }

    private ToeicQuestionGroup parseToeicQuestionFromDocument(final Document document) {
        final ToeicQuestionGroup toeicQuestionGroup = new ToeicQuestionGroup();
        final ToeicQuestion toeicQuestion = new ToeicQuestion();
        final Element contextWrapper = document.getElementsByClass("context-wrapper").get(0);
        final Element questionWrapper = document.getElementsByClass("question-wrapper").get(0);

        toeicQuestionGroup.setQuestions(List.of(toeicQuestion));

        // 1. Get audio
        final Element audioElement = contextWrapper.getElementsByTag("source").get(0);
        final String fixedAudioSource = CrawlStudy4Utils.fixAssetUrl(audioElement.attr("src"));

        final ToeicItemContent audioContent = new ToeicItemContent();
        audioContent.setType("AUDIO");
        audioContent.setContent(fixedAudioSource);

        toeicQuestionGroup.setQuestionContents(List.of(audioContent));

        // 2. Get question
        final String rawTranscript = contextWrapper.text();
        final String questionContent = this.getQuestionRawTranscript(rawTranscript);
        toeicQuestion.setQuestion(questionContent);
        //log.debug(questionContent);

        // 3. Get 3 choice
        List<ToeicAnswerChoice> choices = this.parseAnswerChoices(rawTranscript);
        toeicQuestion.setChoices(choices);

        // 4. Get correct answer
        final Element correctAnswerElement = questionWrapper.getElementsByClass("mt-2 text-success").get(0);
        final String correctAnswer = Arrays.stream(correctAnswerElement.text().split(":")).toList().get(1);

        if (!(correctAnswer.length() == 1 && 65 <= correctAnswer.charAt(0) && correctAnswer.charAt(0) <= 65 + 4 - 1)) {
            throw new RuntimeException("Unknown correct answer format: " + correctAnswer);
        }

        // 5. Get question id
        final Element questionIdElement = questionWrapper.getElementsByTag("strong").get(0);
        toeicQuestion.setQuestionNumber(Integer.parseInt(questionIdElement.text()));

        toeicQuestion.setCorrectAnswer(correctAnswer);

        return toeicQuestionGroup;
    }

    public ToeicPart crawl(List<String> urls) throws IOException {
        //log.debug("Begin crawl part 2 ");
        ToeicPart toeicPart = new ToeicPart();
        toeicPart.setPartNumber(2);

        final List<Document> documents = CrawlStudy4Utils.getAllDocumentFromUrls(urls);
        final List<ToeicQuestionGroup> toeicQuestionGroups = new ArrayList<>();

        for (Document document : documents) {
            final ToeicQuestionGroup questionGroup = this.parseToeicQuestionFromDocument(document);
            toeicQuestionGroups.add(questionGroup);
        }

        toeicPart.setGroups(toeicQuestionGroups);

        ////log.info(gsonInstance.toJson(toeicPart));

        //log.debug("End crawl part 2 ");
        return toeicPart;
    }
}
