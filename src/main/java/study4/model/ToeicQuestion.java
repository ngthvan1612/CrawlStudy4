package study4.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.List;

@Data
public class ToeicQuestion implements Serializable {
    @SerializedName("question_id")
    private Integer questionNumber;
    private String question;
    private List<ToeicAnswerChoice> choices;
    @SerializedName("correct_answer")
    private String correctAnswer;
    private String explain;

    public ToeicQuestion() {

    }
}
