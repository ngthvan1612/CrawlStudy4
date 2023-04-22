package study4.crawl.part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import study4.model.*;

import org.jsoup.nodes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CrawlPartSix {
    private final static Gson gsonInstance = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class QuestionContextWithExplainModel {
        private Element contextWrapper;
        private Element questionWrapper;
        private Element questionExplanationWrapper;
    }

    public CrawlPartSix() {

    }

    private List<QuestionContextWithExplainModel> parseQuestionContextModelFromRoot(Element root) {
        final List<QuestionContextWithExplainModel> result = new ArrayList<>();
        QuestionContextWithExplainModel model = null;

        for (final Element elm : root.children()) {
            //System.out.println("----> " + elm.attr("class"));
            if (elm.attr("class").equals("context-wrapper")) {
                if (model != null) {
                    result.add(model);
                }

                model = new QuestionContextWithExplainModel();
                model.setContextWrapper(elm);
            }
            else if (elm.attr("class").equals("question-wrapper")) {
                assert model != null;
                assert model.getContextWrapper() != null;
                model.setQuestionWrapper(elm);
            }
            else if (elm.attr("class").equals("question-explanation-wrapper")) {
                assert model != null;
                assert model.getContextWrapper() != null;
                assert model.getQuestionWrapper() != null;
                model.setQuestionExplanationWrapper(elm);

                result.add(model);

                model = null;
            }
        }

        if (model != null) {
            result.add(model);
        }

        assert result.size() == 4;
        return result;
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

    private ToeicQuestion parseQuestionModel(QuestionContextWithExplainModel model) {
        final ToeicQuestion toeicQuestion = new ToeicQuestion();

        // 1. Get question id
        final int questionId = Integer.parseInt(model.getQuestionWrapper().getElementsByTag("strong").text().strip());
        assert 131 <= questionId && questionId <= 131 + 16 - 1;
        toeicQuestion.setQuestionNumber(questionId);

        // 2. Question context
        if (model.getQuestionWrapper().getElementsByClass("question-text").size() > 0) {
            final Element questionTextElement = model.getQuestionWrapper().getElementsByClass("question-text").get(0);
            final String questionText = questionTextElement.text().strip();
            //System.out.println("---------> " + questionText);
            toeicQuestion.setQuestion(questionText);
        }

        // 3. Get 4 answer
        final Element questionAnswersDiv = model.getQuestionWrapper().getElementsByClass("question-answers").get(0);
        toeicQuestion.setChoices(this.parseAnswerChoices(questionAnswersDiv));

        // 4. Get correct answer
        final Element correctAnswerElement = model.getQuestionWrapper().getElementsByClass("mt-2 text-success").get(0);
        final String correctAnswer = Arrays.stream(correctAnswerElement.text().split(":")).toList().get(1);

        if (!(correctAnswer.length() == 1 && 65 <= correctAnswer.charAt(0) && correctAnswer.charAt(0) <= 65 + 4 - 1)) {
            throw new RuntimeException("Unknown correct answer format: " + correctAnswer);
        }

        // 5. Get explanation
        if (model.getQuestionExplanationWrapper() != null) {
            Element innerQuestionExplanationElement = null;

            if (model.getQuestionExplanationWrapper().getElementsByClass("collapse show").size() > 0) {
                innerQuestionExplanationElement = model.getQuestionExplanationWrapper().getElementsByClass("collapse show").get(0);
            } else {
                innerQuestionExplanationElement = model.getQuestionExplanationWrapper().getElementsByClass("collapse").get(0);
            }
            toeicQuestion.setExplain(innerQuestionExplanationElement.toString());
        }

        toeicQuestion.setCorrectAnswer(correctAnswer);

        return toeicQuestion;
    }

    private ToeicQuestionGroup parseQuestionGroup(Element root) {
        final ToeicQuestionGroup toeicQuestionGroup = new ToeicQuestionGroup();

        // 1. Get question content
        assert root.getElementsByClass("context-content text-highlightable").size() == 1;
        assert root.getElementsByClass("context-content context-transcript text-highlightable").size() == 1;

        final Element transcriptEnglishElement = root.getElementsByClass("context-content text-highlightable").get(0);
        final Element transcriptVietNamElement = root.getElementsByClass("context-content context-transcript text-highlightable").get(0);

        ToeicItemContent itemContentTranscriptEnglish = new ToeicItemContent();
        itemContentTranscriptEnglish.setType("HTML");
        itemContentTranscriptEnglish.setContent(transcriptEnglishElement.toString());

        ToeicItemContent itemContentTranscriptVietNam = new ToeicItemContent();
        itemContentTranscriptVietNam.setType("HTML");
        itemContentTranscriptVietNam.setContent(transcriptVietNamElement.toString());

        // 2. Get 4 child question
        assert root.getElementsByClass("questions-wrapper").size() == 1;
        List<QuestionContextWithExplainModel> models = this.parseQuestionContextModelFromRoot(
                root.getElementsByClass("questions-wrapper").get(0)
        );

        toeicQuestionGroup.setQuestions(models.stream().map(this::parseQuestionModel).toList());

        assert models.size() == 4;

        return toeicQuestionGroup;
    }

    public ToeicPart crawl(Element root) {
        //log.debug("Begin crawl part 6");
        final ToeicPart toeicPart = new ToeicPart();
        toeicPart.setPartNumber(6);

        List<Element> questionGroupElements =root.getElementsByClass("question-twocols");

        assert questionGroupElements.size() == 4;

        List<ToeicQuestionGroup> toeicQuestionGroups = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            ToeicQuestionGroup toeicQuestionGroup = this.parseQuestionGroup(questionGroupElements.get(i));
            toeicQuestionGroups.add(toeicQuestionGroup);
        }
        toeicPart.setGroups(toeicQuestionGroups);

        //System.out.println(gsonInstance.toJson(toeicPart));

        //log.debug("End crawl part 6");
        return toeicPart;
    }
}
