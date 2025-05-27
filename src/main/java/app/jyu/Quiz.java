package app.jyu;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

public class Quiz implements Serializable {
    // {
    //     "question": "n. 环境",
    //     "options": [
    //       "environment",
    //       "instrument",
    //       "argument",
    //       "entertainment"
    //     ],
    //     "answer": 1,
    //     "uuid": "f9a7e1d0-c1b3-4e5a-9f0e-7d8c6b5a4f3c"
    //   },
    public String question;
    public String[] options;
    public Integer answer; // 0-based
    public UUID uuid;

    // Temporary class to match JSON structure for GSON deserialization
    private static class QuizJsonItem {
        @SerializedName("question")
        String questionText;
        @SerializedName("options")
        String[] optionList;
        @SerializedName("answer")
        int answerIndex; // 0-based index
        @SerializedName("uuid")
        String uuidString;
    }

    public static ConcurrentHashMap<UUID, Quiz> getQuizMap() {
        ConcurrentHashMap<UUID, Quiz> quizMap = new ConcurrentHashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<List<QuizJsonItem>>() {}.getType();

        try (InputStream inputStream = Quiz.class.getClassLoader().getResourceAsStream("assets/ping_system/quiz/cet4.json");
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            if (inputStream == null) {
                System.err.println("Cannot find the quiz file.");
                return quizMap; // Return empty map or throw an exception
            }
            List<QuizJsonItem> quizItems = gson.fromJson(reader, listType);
            for (QuizJsonItem item : quizItems) {
                Quiz quiz = new Quiz();
                quiz.question = item.questionText;
                quiz.options = item.optionList;
                quiz.answer = item.answerIndex; // The JSON answer is 1-based
                quiz.uuid = UUID.fromString(item.uuidString);
                quizMap.put(quiz.uuid, quiz);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle exception: log it, or rethrow as a runtime exception
        }
        return quizMap;
    }

    public static Quiz randNoAnswer(ConcurrentHashMap<UUID, Quiz> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        List<UUID> keys = new ArrayList<>(map.keySet());
        Random random = new Random();
        UUID randomKey = keys.get(random.nextInt(keys.size()));
        var item = map.get(randomKey);
        item.answer = null;
        return item;
    }

    public static boolean isCorrectAns(ConcurrentHashMap<UUID, Quiz> map, UUID uuid, int answerIdx) {
        var item = map.get(uuid);
        return item.answer == answerIdx;
    }
}
