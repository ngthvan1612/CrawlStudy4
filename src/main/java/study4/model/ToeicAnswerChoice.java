package study4.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ToeicAnswerChoice implements Serializable {
    private String label;
    private String content;
    private String explain;

    public ToeicAnswerChoice() {

    }
}
