package study4.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.List;

@Data
public class ToeicQuestion implements Serializable {
    private Integer questionNumber;
    private String question;
    private List<ToeicAnswerChoice> choices;
    private String correctAnswer;
    private String explain;

    public ToeicQuestion() {

    }
}
