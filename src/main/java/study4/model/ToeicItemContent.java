package study4.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ToeicItemContent implements Serializable {
    private String type;
    private String content;

    public ToeicItemContent() {

    }
}
