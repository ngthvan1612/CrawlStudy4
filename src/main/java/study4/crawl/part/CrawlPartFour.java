package study4.crawl.part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import study4.crawl.CrawlStudy4Utils;
import study4.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CrawlPartFour {
    private final static Gson gsonInstance = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public CrawlPartFour() {

    }

    private ToeicItemContent parseTranscript(List<Document> documents) {
        final Document doc = documents.get(0);
        final ToeicItemContent transcriptContent = new ToeicItemContent();
        final Element transcriptElement = doc.getElementsByClass("context-content context-transcript ").get(0);

        transcriptContent.setType("HTML");
        transcriptContent.setContent(CrawlStudy4Utils.formatHtml(transcriptElement.toString()));

        return transcriptContent;
    }

    private ToeicItemContent parseAudio(List<Document> documents) {
        final Document doc = documents.get(0);
        final ToeicItemContent transcriptContent = new ToeicItemContent();
        final Element audioElement = doc.getElementsByTag("source").get(0);
        final String fixedAudioSource = CrawlStudy4Utils.fixAssetUrl(audioElement.attr("src"));

        transcriptContent.setType("AUDIO");
        transcriptContent.setContent(fixedAudioSource);

        return transcriptContent;
    }

    private List<ToeicAnswerChoice> parseAnswerChoices(Element questionAnswersDiv) {
        List<ToeicAnswerChoice> result = new ArrayList<>();

        for (Element element : questionAnswersDiv.getElementsByClass("form-check-label")) {
            final String line = element.text();

            assert line.startsWith("A.") || line.startsWith("B.") || line.startsWith("C.") || line.startsWith("D.");

            ToeicAnswerChoice choice = new ToeicAnswerChoice();
            choice.setLabel(line.charAt(0) + "");
            choice.setContent(line.substring(2).strip());

            result.add(choice);
        }

        if (result.size() != 4) {
            throw new RuntimeException("Wrong number of choices [" + result.size() + "]");
        }

        return result;
    }

    private ToeicQuestion parseSingleQuestion(Document document) {
        final ToeicQuestion toeicQuestion = new ToeicQuestion();
        final Element questionWrapper = document.getElementsByClass("question-wrapper").get(0);

        // 1. Get question text
        final String questionText = questionWrapper.getElementsByClass("question-text").get(0).text().strip();
        assert questionText != null && questionText.length() > 0;
        toeicQuestion.setQuestion(questionText);

        // 2. Get question id
        final Integer questionNumber = Integer.parseInt(questionWrapper.getElementsByTag("strong").get(0).text().strip());
        toeicQuestion.setQuestionNumber(questionNumber);

        // 3. Get all choices
        final Element questionAnswersDiv = questionWrapper.getElementsByClass("question-answers").get(0);
        toeicQuestion.setChoices(this.parseAnswerChoices(questionAnswersDiv));

        // 4. Get correct answer
        final Element correctAnswerElement = questionWrapper.getElementsByClass("mt-2 text-success").get(0);
        final String correctAnswer = Arrays.stream(correctAnswerElement.text().split(":")).toList().get(1);

        if (!(correctAnswer.length() == 1 && 65 <= correctAnswer.charAt(0) && correctAnswer.charAt(0) <= 65 + 4 - 1)) {
            throw new RuntimeException("Unknown correct answer format: " + correctAnswer);
        }

        toeicQuestion.setCorrectAnswer(correctAnswer);

        return toeicQuestion;
    }

    private ToeicQuestionGroup parseToeicQuestionGroupFromDocument(List<Document> documents) {
        final ToeicQuestionGroup toeicQuestionGroup = new ToeicQuestionGroup();

        // 1. Get audio
        toeicQuestionGroup.setQuestionContents(List.of(this.parseAudio(documents)));

        // 2. Get transcript
        toeicQuestionGroup.setTranscript(List.of(this.parseTranscript(documents)));

        // 3. Parse 3 questions
        List<ToeicQuestion> toeicQuestions = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            final ToeicQuestion toeicQuestion = this.parseSingleQuestion(documents.get(i));
            toeicQuestions.add(toeicQuestion);
        }
        toeicQuestionGroup.setQuestions(toeicQuestions);

        final int x = toeicQuestions.get(0).getQuestionNumber();
        final int y = toeicQuestions.get(1).getQuestionNumber();
        final int z = toeicQuestions.get(2).getQuestionNumber();
        assert y == x + 1 && z == y + 1;

        return toeicQuestionGroup;
    }

    public ToeicPart crawl(List<String> urls) throws IOException {
        assert urls.size() == 30;

        //log.debug("Begin crawl part 4 ");
        ToeicPart toeicPart = new ToeicPart();
        toeicPart.setPartNumber(4);

        final List<Document> documents = CrawlStudy4Utils.getAllDocumentFromUrls(urls);
        final List<ToeicQuestionGroup> toeicQuestionGroups = new ArrayList<>();

        for (int i = 0; i < 30; i += 3) {
            List<Document> docs = new ArrayList<>();
            docs.add(documents.get(i));
            docs.add(documents.get(i + 1));
            docs.add(documents.get(i + 2));
            final ToeicQuestionGroup questionGroup = this.parseToeicQuestionGroupFromDocument(docs);
            toeicQuestionGroups.add(questionGroup);
        }

        toeicPart.setGroups(toeicQuestionGroups);

        //log.info(gsonInstance.toJson(toeicPart));

        //log.debug("End crawl part 4 ");
        return toeicPart;
    }
}
