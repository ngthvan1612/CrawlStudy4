package study4.crawl.part;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import study4.model.ToeicAnswerChoice;
import study4.model.ToeicPart;
import org.jsoup.nodes.*;
import study4.model.ToeicQuestion;
import study4.model.ToeicQuestionGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CrawlPartFive {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class QuestionContextWithExplainModel {
        private Element contextWrapper;
        private Element questionWrapper;
        private Element questionExplanationWrapper;
    }

    private final static Gson gsonInstance = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public CrawlPartFive() {

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

    private List<QuestionContextWithExplainModel> parseQuestionContextModelFromRoot(Element root) {
        final List<QuestionContextWithExplainModel> result = new ArrayList<>();
        QuestionContextWithExplainModel model = null;

        for (final Element elm : root.children()) {
            if (elm.attr("class").equals("context-wrapper")) {
                assert model == null;
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

        assert result.size() == 30;
        return result;
    }

    private ToeicQuestionGroup parseQuestionFromElement(QuestionContextWithExplainModel model) {
        final ToeicQuestionGroup toeicQuestionGroup = new ToeicQuestionGroup();
        final ToeicQuestion toeicQuestion = new ToeicQuestion();
        toeicQuestionGroup.setQuestions(List.of(toeicQuestion));

        // 1. Get question id
        final int questionId = Integer.parseInt(model.getQuestionWrapper().getElementsByTag("strong").text().strip());
        assert 101 <= questionId && questionId <= 101 + 30 - 1;
        toeicQuestion.setQuestionNumber(questionId);

        // 2. Question context
        final Element questionTextElement = model.getQuestionWrapper().getElementsByClass("question-text").get(0);
        final String questionText = questionTextElement.text().strip();
        assert questionText != null && questionText.length() > 20;
        toeicQuestion.setQuestion(questionText);

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
        Element innerQuestionExplanationElement = null;

        if (model.getQuestionExplanationWrapper().getElementsByClass("collapse show").size() > 0) {
            innerQuestionExplanationElement = model.getQuestionExplanationWrapper().getElementsByClass("collapse show").get(0);
        }
        else {
            innerQuestionExplanationElement = model.getQuestionExplanationWrapper().getElementsByClass("collapse").get(0);
        }
        toeicQuestion.setExplain(innerQuestionExplanationElement.toString());

        toeicQuestion.setCorrectAnswer(correctAnswer);

        return toeicQuestionGroup;
    }

    public ToeicPart crawl(Element root) {
        //log.debug("Begin crawl part 5");
        ToeicPart toeicPart = new ToeicPart();
        toeicPart.setPartNumber(5);

        assert root.getElementsByClass("test-questions-wrapper").size() == 1;
        root = root.getElementsByClass("test-questions-wrapper").get(0);

        final List<QuestionContextWithExplainModel> questionGroupElement = this.parseQuestionContextModelFromRoot(root);
        List<ToeicQuestionGroup> toeicQuestionGroups = new ArrayList<>();
        for (QuestionContextWithExplainModel questionContextWithExplainModel : questionGroupElement) {
            final ToeicQuestionGroup toeicQuestionGroup = this.parseQuestionFromElement(questionContextWithExplainModel);
            toeicQuestionGroups.add(toeicQuestionGroup);
        }

        toeicPart.setGroups(toeicQuestionGroups);

        //System.out.println(gsonInstance.toJson(toeicPart));

        //log.debug("End crawl part 5");
        return toeicPart;
    }
}
