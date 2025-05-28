package app.jyu;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

public class Book implements Serializable {
    
    public static class Meaning implements Serializable {
        @SerializedName("meaning")
        public String meaning;
        @SerializedName("type")
        public int type; // 0: Adjective, 1: Noun, 2: Adverb, 3: Verb
        
        public Meaning() {}
        
        public Meaning(String meaning, int type) {
            this.meaning = meaning;
            this.type = type;
        }
    }
    
    public static class Word implements Serializable {
        @SerializedName("wID")
        public int wID;
        @SerializedName("word")
        public String word;
        @SerializedName("meanings")
        public List<Meaning> meanings;
        
        public Word() {
            this.meanings = new ArrayList<>();
        }
        
        public Word(int wID, String word, List<Meaning> meanings) {
            this.wID = wID;
            this.word = word;
            this.meanings = meanings != null ? meanings : new ArrayList<>();
        }
    }
    
    @SerializedName("bID")
    public int bID;
    @SerializedName("words")
    public List<Word> words;
    
    private static volatile CopyOnWriteArrayList<Book> BOOKS;
    private static final Random rg = new Random();
    
    // Book names mapping for file loading
    private static final String[] BOOK_FILES = {
        "cet_4.json",
        "cet_6.json", 
        "cet_6_high_frequency.json",
        "cet_4_must_2000.json",
        "cet_4_core.json",
        "cet_6_core.json",
        "cet_phrase_core.json",
        "tem_4.json",
        "tem_4_core_light.json",
        "tem_8.json",
        "neep.json",
        "neep2.json",
        "neep2_core.json",
        "ielts.json",
        "ielts_core.json",
        "toefl.json",
        "toefl_core.json",
        "toefl_core_light.json"
    };
    
    public Book() {
        this.words = new ArrayList<>();
    }
    
    public Book(int bID, List<Word> words) {
        this.bID = bID;
        this.words = words != null ? words : new ArrayList<>();
    }
    
    public static CopyOnWriteArrayList<Book> loadBooks() {
        QuizCraft.LOGGER.info("Loading books");
        // Double-Checked Locking for thread-safe lazy initialization
        if (BOOKS == null) {
            synchronized (Book.class) {
                if (BOOKS == null) {
                    CopyOnWriteArrayList<Book> books = new CopyOnWriteArrayList<>();
                    Gson gson = new Gson();
                    
                    for (String fileName : BOOK_FILES) {
                        String resourcePath = "assets/quiz_craft/books/" + fileName;
                        
                        try (InputStream inputStream = Book.class.getClassLoader().getResourceAsStream(resourcePath);
                             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                            
                            if (inputStream == null) {
                                QuizCraft.LOGGER.warn("Cannot find book file: {}", resourcePath);
                                continue;
                            }
                            
                            Book book = gson.fromJson(reader, Book.class);
                            if (book != null) {
                                books.add(book);
                                QuizCraft.LOGGER.info("Loaded book: bID={}, words={}", book.bID, book.words.size());
                            }
                            
                        } catch (Exception e) {
                            QuizCraft.LOGGER.error("Error loading book file: {}", resourcePath, e);
                        }
                    }
                    
                    BOOKS = books;
                }
            }
        }
        
        // Log summary
        if (BOOKS != null && !BOOKS.isEmpty()) {
            int totalWords = BOOKS.stream().mapToInt(book -> book.words.size()).sum();
            QuizCraft.LOGGER.info("Books loaded. Total books: {}, Total words: {}", BOOKS.size(), totalWords);
        } else {
            QuizCraft.LOGGER.info("Books loaded but list is empty.");
        }
        
        return BOOKS;
    }
    
    public static Book getBookById(int bID) {
        if (BOOKS == null || BOOKS.isEmpty()) {
            QuizCraft.LOGGER.error("Books not loaded or empty");
            return null;
        }
        
        return BOOKS.stream()
                .filter(book -> book.bID == bID)
                .findFirst()
                .orElse(null);
    }
    
    public static Word getRandomWordFromBook(int bID) {
        Book book = getBookById(bID);
        if (book == null || book.words.isEmpty()) {
            QuizCraft.LOGGER.warn("Book {} not found or has no words", bID);
            return null;
        }
        
        return book.words.get(rg.nextInt(book.words.size()));
    }

    public static Quiz getRandomQuiz(int bID) {
        Book book = getBookById(bID);
        if (book == null || book.words.isEmpty()) {
            QuizCraft.LOGGER.warn("Book {} not found or has no words", bID);
            return null;
        }
        int ans_idx = rg.nextInt(4);
        var four_words = rg.ints(0, book.words.size()).distinct().limit(4).mapToObj(book.words::get).toArray(Word[]::new);
        var four_options = Arrays.stream(four_words).map(word -> word.word).toArray(String[]::new);
        var ask_meaning = four_words[ans_idx].meanings.get(rg.nextInt(four_words[ans_idx].meanings.size()));
        // get quiz
        var quiz = new Quiz(ask_meaning.meaning, four_options, ans_idx);
        return quiz;
    }
    
    public static String getTypeString(int type) {
        switch (type) {
            case 0: return "adj."; // Adjective
            case 1: return "n.";   // Noun
            case 2: return "adv."; // Adverb
            case 3: return "v.";   // Verb
            default: return "unknown";
        }
    }
} 