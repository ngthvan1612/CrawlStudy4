package study4.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ToeicPart implements Serializable {
    private Integer partNumber;
    private List<ToeicQuestionGroup> groups;

    public ToeicPart() {

    }
}
