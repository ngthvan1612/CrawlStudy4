package study4.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ToeicFullTest implements Serializable {
    private String slug;
    private String fullName;
    private List<ToeicPart> parts;

    public ToeicFullTest() {

    }
}
