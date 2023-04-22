package study4.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.io.File;
import java.io.Serializable;
import java.util.List;

@Data
public class ToeicQuestionGroup implements Serializable {
    private List<ToeicItemContent> questionContents;
    private List<ToeicItemContent> transcript;
    private List<ToeicQuestion> questions;

    public ToeicQuestionGroup() {

    }
}
