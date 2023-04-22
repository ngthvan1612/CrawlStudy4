package study4.crawl;

import com.google.gson.Gson;
import study4.model.ToeicFullTest;
import study4.model.ToeicItemContent;
import study4.model.ToeicPart;
import study4.model.ToeicQuestionGroup;
import study4.network.CachedNetworkOperation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipToeicFullTest {
    private final CachedNetworkOperation cachedNetworkOperation = CachedNetworkOperation.getInstance();
    private static final Gson gsonInstance = new Gson();

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

    public ByteArrayOutputStream zipFile(ToeicFullTest toeicFullTestRaw) throws IOException {
        assert toeicFullTestRaw != null;

        final ToeicFullTest toeicFullTest = gsonInstance.fromJson(gsonInstance.toJson(toeicFullTestRaw), ToeicFullTest.class);

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        // write /data
        for (ToeicPart part : toeicFullTest.getParts()) {
            for (ToeicQuestionGroup group : part.getGroups()) {
                if (group.getQuestionContents() == null)
                    continue;;

                for (ToeicItemContent itemContent : group.getQuestionContents()) {
                    if (itemContent.getType().equals("AUDIO") || itemContent.getType().equals("IMAGE")) {
                        final String ext = itemContent.getType().equals("AUDIO") ? ".mp3" : ".png";
                        final String url = itemContent.getContent();
                        final byte[] data = cachedNetworkOperation.downloadStream(url);
                        final String newName = "data/" + this.joinQuestionGroupNumbers(group) + ext;

                        ZipEntry zipEntry = new ZipEntry(newName);
                        zipOutputStream.putNextEntry(zipEntry);
                        zipOutputStream.write(data);
                        zipOutputStream.closeEntry();

                        itemContent.setContent(newName);
                    }
                }
            }
        }

        // write config.json
        ZipEntry configEntry = new ZipEntry("config.json");
        zipOutputStream.putNextEntry(configEntry);
        zipOutputStream.write(gsonInstance.toJson(toeicFullTest).getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();

        zipOutputStream.close();
        return outputStream;
    }
}
