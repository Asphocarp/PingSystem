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

    private static volatile ConcurrentHashMap<UUID, Quiz> QUIZ_MAP;
    // private static volatile CopyOnWriteArrayList<Book> BOOKS;
    private static final Random rg = new Random();

    public Quiz(String question, String[] options, int answer) {
        this.question = question;
        this.options = options;
        this.answer = answer;
        this.uuid = UUID.randomUUID();
    }

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

    public static ConcurrentHashMap<UUID, Quiz> loadQuizMap() {
        // load quiz map from file assets/quiz_craft/quiz/cet4.json (only once)
        QuizCraft.LOGGER.info("Loading quiz map");
        // Double-Checked Locking for thread-safe lazy initialization
        if (QUIZ_MAP == null) {
            synchronized (Quiz.class) {
                if (QUIZ_MAP == null) {
                    ConcurrentHashMap<UUID, Quiz> quizMap = new ConcurrentHashMap<>();
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<QuizJsonItem>>() {}.getType();

                    try (InputStream inputStream = Quiz.class.getClassLoader().getResourceAsStream("assets/quiz_craft/quiz/cet4.json");
                         Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        if (inputStream == null) {
                            QuizCraft.LOGGER.error("Cannot find the quiz file: assets/quiz_craft/quiz/cet4.json");
                            // Return an empty map or throw a specific exception if the file is critical
                            QUIZ_MAP = new ConcurrentHashMap<>(); // Initialize to empty to avoid repeated attempts
                            return QUIZ_MAP;
                        }
                        List<QuizJsonItem> quizItems = gson.fromJson(reader, listType);
                        for (QuizJsonItem item : quizItems) {
                            Quiz quiz = new Quiz(item.questionText, item.optionList, item.answerIndex);
                            quizMap.put(quiz.uuid, quiz);
                        }
                        QUIZ_MAP = quizMap;
                    } catch (Exception e) {
                        QuizCraft.LOGGER.error("Error loading quiz map", e);
                        // In case of an error, initialize to an empty map or rethrow
                        QUIZ_MAP = new ConcurrentHashMap<>();
                    }
                }
            }
        }
        // log (for debug)
        if (!QUIZ_MAP.isEmpty()) {
            Quiz firstQuiz = QUIZ_MAP.values().iterator().next();
            QuizCraft.LOGGER.info("Quiz map loaded. First item: question='{}', options={}, answer={}, uuid={}",
                firstQuiz.question, java.util.Arrays.toString(firstQuiz.options), firstQuiz.answer, firstQuiz.uuid);
        } else {
            QuizCraft.LOGGER.info("Quiz map loaded but is empty.");
        }
        return QUIZ_MAP;
    }

    public static Quiz randNoAnswer() {
        if (QUIZ_MAP == null || QUIZ_MAP.isEmpty()) {
            QuizCraft.LOGGER.error("Quiz map is empty");
            return null;
        }
        List<UUID> keys = new ArrayList<>(QUIZ_MAP.keySet());
        UUID randomKey = keys.get(rg.nextInt(keys.size()));
        var item = QUIZ_MAP.get(randomKey);
        item.answer = null;
        return item;
    }

    public boolean isCorrectAnswer(int answerIdx) {
        return answer == answerIdx;
    }
}
