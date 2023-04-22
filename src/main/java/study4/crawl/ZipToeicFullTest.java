package study4.crawl;

import study4.model.ToeicFullTest;
import study4.model.ToeicItemContent;
import study4.model.ToeicPart;
import study4.model.ToeicQuestionGroup;
import study4.network.CachedNetworkOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

public class ZipToeicFullTest {
    private final CachedNetworkOperation cachedNetworkOperation = CachedNetworkOperation.getInstance();

    private String joinQuestionGroupNumbers(ToeicQuestionGroup group) {
        List<String> ids = group
                .getQuestions().stream().map(question -> question.getQuestionNumber().toString())
                .toList();

        return String.join("-", ids);
    }

    private String getImageFileName(String joinedQuestionNumbers) {
        return joinedQuestionNumbers + ".png";
    }

    private String getAudioFileName(String joinedQuestionNumbers) {
        return joinedQuestionNumbers + ".mp3";
    }

    public void zipFile(ToeicFullTest toeicFullTest) throws IOException {
        assert toeicFullTest != null;
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        for (ToeicPart part : toeicFullTest.getParts()) {
            for (ToeicQuestionGroup group : part.getGroups()) {
                if (group.getQuestionContents() == null)
                    continue;;

                for (ToeicItemContent itemContent : group.getQuestionContents()) {
                    if (itemContent.getType().equals("AUDIO") || itemContent.getType().equals("IMAGE")) {
                        final String url = itemContent.getContent();
                        final byte[] data = cachedNetworkOperation.downloadStream(url);
                        System.out.println("DATA " + url + " -> " + data.length + " bytes");
                    }
                }
            }
        }

        outputStream.close();
        zipOutputStream.close();
    }
}
