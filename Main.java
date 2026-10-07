import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/*
 * ADAPTIVE TIMED QUIZ SYSTEM
 * Single-file Java project.
 *
 * Default Student : smriti / 123
 * Default Admin   : admin / admin
 *
 * Recommended: Java 21+
 *
 * INPUT DESIGN NOTE:
 * All console input (menus AND timed quiz answers) is read through ONE
 * dedicated background thread that continuously reads lines from System.in
 * and pushes them onto a BlockingQueue. Every place in the program that
 * needs input (readLine, readInt, the timed quiz answer) takes from that
 * same queue instead of calling Scanner directly. This is what the old
 * version got wrong: it kept spinning up a NEW thread that raced a Scanner
 * against the main thread for every single question, which is exactly what
 * corrupted the Scanner's internal buffer and caused the
 * IndexOutOfBoundsException crash you saw when returning to the Student
 * Menu. With one reader thread and one queue, there is never more than one
 * piece of code touching System.in at a time.
 */

public class Main {

    public static void main(String[] args) {
        QuizSystem system = new QuizSystem();
        system.start();
    }

    // =========================
    // ENUM
    // =========================

    enum Difficulty {
        EASY, MEDIUM, HARD;

        public Difficulty harder() {
            if (this == EASY) return MEDIUM;
            if (this == MEDIUM) return HARD;
            return HARD;
        }

        public Difficulty easier() {
            if (this == HARD) return MEDIUM;
            if (this == MEDIUM) return EASY;
            return EASY;
        }
    }

    // =========================
    // ABSTRACT CLASS
    // =========================

    static abstract class User {
        private final String username;
        private final String password;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public boolean login(String u, String p) {
            return username.equals(u) && password.equals(p);
        }

        public abstract void displayRole();
    }

    // =========================
    // STUDENT
    // =========================

    static class Student extends User implements Cloneable {
        private ArrayList<Integer> scores;

        public Student(String username, String password) {
            super(username, password);
            scores = new ArrayList<>();
        }

        @Override
        public void displayRole() {
            System.out.println("Logged in as Student");
        }

        public void addScore(int score) {
            scores.add(score);
        }

        public ArrayList<Integer> getScores() {
            return scores;
        }

        // Deep copy of the score list
        @Override
        public Student clone() {
            try {
                Student copy = (Student) super.clone();
                copy.scores = new ArrayList<>(this.scores);
                return copy;
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Cloning failed", e);
            }
        }
    }

    // =========================
    // FINAL CLASS
    // =========================

    static final class Admin extends User {
        public Admin(String username, String password) {
            super(username, password);
        }

        @Override
        public void displayRole() {
            System.out.println("Logged in as Admin");
        }

        public final void createQuiz() {
            System.out.println("Quiz Management Access Granted");
        }
    }

    // =========================
    // INTERFACE
    // =========================

    interface ResultCalculator {
        int calculate(int correctAnswers, int totalQuestions);
    }

    // =========================
    // RECORD
    // =========================

    record Question(
            int id,
            String topic,
            String text,
            String A,
            String B,
            String C,
            String D,
            char answer,
            Difficulty difficulty) {
    }

    // =========================
    // CUSTOM EXCEPTION
    // =========================

    static class InvalidQuestionException extends Exception {
        public InvalidQuestionException(String message) {
            super(message);
        }
    }

    // =========================
    // QUESTION BANK
    // =========================

    static class QuestionBank {
        private final ArrayList<Question> questions = new ArrayList<>();
        private int nextId = 1;

        public QuestionBank() {
            loadDefaultQuestions();
        }

        public ArrayList<Question> getQuestions() {
            return questions;
        }

        private void addDefault(String topic, String text,
                                String a, String b, String c, String d,
                                char answer, Difficulty difficulty) {
            questions.add(new Question(nextId++, topic, text, a, b, c, d,
                    Character.toUpperCase(answer), difficulty));
        }

        private void loadDefaultQuestions() {

            // EASY
            addDefault("Java", "Who developed Java?",
                    "James Gosling", "Dennis Ritchie", "Guido van Rossum", "Bjarne Stroustrup",
                    'A', Difficulty.EASY);

            addDefault("Java", "Which keyword is used to create an object?",
                    "new", "class", "object", "create",
                    'A', Difficulty.EASY);

            addDefault("OOP", "Which concept hides implementation details?",
                    "Inheritance", "Abstraction", "Polymorphism", "Compilation",
                    'B', Difficulty.EASY);

            addDefault("Collections", "Which collection does not allow duplicate elements?",
                    "ArrayList", "HashSet", "LinkedList", "Vector",
                    'B', Difficulty.EASY);

            addDefault("Exceptions", "Which block handles an exception?",
                    "try", "catch", "throw", "final",
                    'B', Difficulty.EASY);

            addDefault("Streams", "Which method converts a stream into a result list in modern Java?",
                    "collect", "scan", "convert", "store",
                    'A', Difficulty.EASY);

            // MEDIUM
            addDefault("OOP", "Which concept allows one interface to have many implementations?",
                    "Encapsulation", "Polymorphism", "Compilation", "Package",
                    'B', Difficulty.MEDIUM);

            addDefault("Java", "Which keyword prevents a class from being inherited?",
                    "static", "abstract", "final", "private",
                    'C', Difficulty.MEDIUM);

            addDefault("Collections", "Average lookup in a HashMap is generally:",
                    "O(1)", "O(n)", "O(log n)", "O(n\u00b2)",
                    'A', Difficulty.MEDIUM);

            addDefault("Exceptions", "Which keyword explicitly raises an exception?",
                    "throws", "throw", "catch", "error",
                    'B', Difficulty.MEDIUM);

            addDefault("Streams", "Which operation filters stream elements?",
                    "map", "filter", "reduce", "forEach",
                    'B', Difficulty.MEDIUM);

            addDefault("Threads", "Which interface can be used to define a task for a thread?",
                    "Runnable", "Serializable", "Comparable", "Cloneable",
                    'A', Difficulty.MEDIUM);

            // HARD
            addDefault("Java", "Which feature was introduced as a standard feature in Java 16?",
                    "Records", "Packages", "Primitive types", "Constructors",
                    'A', Difficulty.HARD);

            addDefault("Threads", "Virtual threads became a final feature in which Java release?",
                    "Java 8", "Java 11", "Java 17", "Java 21",
                    'D', Difficulty.HARD);

            addDefault("Streams", "Which stream operation combines elements into one value?",
                    "filter", "map", "reduce", "peek",
                    'C', Difficulty.HARD);

            addDefault("OOP", "Which mechanism is used when a subclass provides its own implementation of a superclass method?",
                    "Overloading", "Overriding", "Hiding", "Casting",
                    'B', Difficulty.HARD);
        }

        public void addQuestion(String topic, String text,
                                String a, String b, String c, String d,
                                char answer, Difficulty difficulty)
                throws InvalidQuestionException {

            if (topic == null || topic.isBlank() ||
                    text == null || text.isBlank() ||
                    a == null || a.isBlank() ||
                    b == null || b.isBlank() ||
                    c == null || c.isBlank() ||
                    d == null || d.isBlank()) {
                throw new InvalidQuestionException("All question fields are required.");
            }

            answer = Character.toUpperCase(answer);

            if (answer < 'A' || answer > 'D') {
                throw new InvalidQuestionException("Correct answer must be A, B, C or D.");
            }

            questions.add(new Question(nextId++, topic.trim(), text.trim(),
                    a.trim(), b.trim(), c.trim(), d.trim(), answer, difficulty));
        }

        public boolean deleteQuestion(int id) {
            return questions.removeIf(q -> q.id() == id);
        }

        public void searchByTopic(String topic) {
            boolean found = false;

            for (Question q : questions) {
                if (q.topic().equalsIgnoreCase(topic)) {
                    printQuestion(q);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("No questions found for topic: " + topic);
            }
        }

        public void showByDifficulty(Difficulty difficulty) {
            boolean found = false;

            for (Question q : questions) {
                if (q.difficulty() == difficulty) {
                    printQuestion(q);
                    found = true;
                }
            }

            if (!found) {
                System.out.println("No questions available.");
            }
        }

        public void showAll() {
            if (questions.isEmpty()) {
                System.out.println("Question bank is empty.");
                return;
            }

            for (Question q : questions) {
                printQuestion(q);
            }
        }

        private void printQuestion(Question q) {
            System.out.println("\n[" + q.id() + "] " + q.topic()
                    + " | " + q.difficulty());
            System.out.println(q.text());
            System.out.println("A. " + q.A());
            System.out.println("B. " + q.B());
            System.out.println("C. " + q.C());
            System.out.println("D. " + q.D());
            System.out.println("Answer: " + q.answer());
        }

        public void saveToFile(String filename) {
            try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
                out.println("ID|Topic|Question|A|B|C|D|Answer|Difficulty");

                for (Question q : questions) {
                    out.println(q.id() + "|" +
                            safe(q.topic()) + "|" +
                            safe(q.text()) + "|" +
                            safe(q.A()) + "|" +
                            safe(q.B()) + "|" +
                            safe(q.C()) + "|" +
                            safe(q.D()) + "|" +
                            q.answer() + "|" +
                            q.difficulty());
                }

                System.out.println("Question bank saved to " + filename);
            } catch (IOException e) {
                System.out.println("Could not save question bank: " + e.getMessage());
            }
        }

        private String safe(String value) {
            return value.replace("|", "/");
        }
    }

    // =========================
    // ANALYTICS + INNER CLASS
    // =========================

    static class Analytics {

        class Report {
            void display() {
                System.out.println();
                System.out.println("Generating Topic Wise Report...");
            }
        }

        public void showTopicPerformance(List<QuestionAttempt> attempts) {
            if (attempts.isEmpty()) {
                System.out.println("No topic data available.");
                return;
            }

            Map<String, int[]> data = new LinkedHashMap<>();

            for (QuestionAttempt attempt : attempts) {
                int[] values = data.computeIfAbsent(
                        attempt.question().topic(),
                        k -> new int[2]);

                if (attempt.correct()) {
                    values[0]++;
                }

                values[1]++;
            }

            System.out.println("\nTOPIC-WISE PERFORMANCE");

            data.forEach((topic, values) -> {
                double percentage = values[1] == 0
                        ? 0
                        : values[0] * 100.0 / values[1];

                System.out.printf("%-15s : %d/%d (%.1f%%)%n",
                        topic, values[0], values[1], percentage);
            });
        }
    }

    record QuestionAttempt(Question question, boolean correct) {
    }

    // =========================
    // QUIZ SYSTEM
    // =========================

    static class QuizSystem {

        // Single shared queue that ALL input (menus + timed answers) is read from.
        private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();

        private final Student student =
                new Student("smriti", "123");

        private final Admin admin =
                new Admin("admin", "admin");

        private final QuestionBank bank =
                new QuestionBank();

        private final ArrayList<QuestionAttempt> currentAttempts =
                new ArrayList<>();

        private final String RESULT_FILE = "quiz_results.csv";
        private final String QUESTION_FILE = "questions.csv";

        public void start() {
            startInputReaderThread();
            loadExistingResults();

            while (true) {
                printMainMenu();

                int choice = readInt("Enter Choice : ");

                switch (choice) {
                    case 1 -> studentLogin();
                    case 2 -> adminLogin();
                    case 3 -> {
                        bank.saveToFile(QUESTION_FILE);
                        System.out.println("Thank You...");
                        return;
                    }
                    default -> System.out.println("Invalid Choice.");
                }
            }
        }

        // =========================
        // INPUT READER THREAD
        // =========================

        /**
         * Starts ONE background thread that owns System.in for the lifetime
         * of the program. It just reads lines and pushes them onto
         * inputQueue. Nothing else in the program ever touches System.in
         * directly - everything reads from the queue instead. This is what
         * prevents the Scanner corruption / IndexOutOfBoundsException bug.
         */
        private void startInputReaderThread() {
            Thread reader = new Thread(() -> {
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(System.in));
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        inputQueue.put(line);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    // stdin closed / unreadable - stop quietly
                }
            }, "input-reader");

            reader.setDaemon(true);
            reader.start();
        }

        private void printMainMenu() {
            System.out.println("\n==========================================");
            System.out.println("       ADAPTIVE TIMED QUIZ SYSTEM");
            System.out.println("==========================================");
            System.out.println("1. Student Login");
            System.out.println("2. Admin Login");
            System.out.println("3. Exit");
        }

        // =========================
        // STUDENT LOGIN
        // =========================

        private void studentLogin() {
            System.out.println("\n----- STUDENT LOGIN -----");

            String username = readLine("Username : ");
            String password = readLine("Password : ");

            if (!student.login(username, password)) {
                System.out.println("Invalid Student Login.");
                return;
            }

            student.displayRole();
            studentMenu();
        }

        private void studentMenu() {
            while (true) {
                System.out.println("\n========== STUDENT MENU ==========");
                System.out.println("1. Start Adaptive Quiz");
                System.out.println("2. View Previous Scores");
                System.out.println("3. View Performance");
                System.out.println("4. Clone Student Profile");
                System.out.println("5. Logout");

                int choice = readInt("Enter Choice : ");

                switch (choice) {
                    case 1 -> takeAdaptiveQuiz();
                    case 2 -> showPreviousScores();
                    case 3 -> performance();
                    case 4 -> cloneStudent();
                    case 5 -> {
                        System.out.println("Student Logged Out.");
                        return;
                    }
                    default -> System.out.println("Invalid Choice.");
                }
            }
        }

        // =========================
        // ADMIN LOGIN
        // =========================

        private void adminLogin() {
            System.out.println("\n----- ADMIN LOGIN -----");

            String username = readLine("Username : ");
            String password = readLine("Password : ");

            if (!admin.login(username, password)) {
                System.out.println("Invalid Admin Login.");
                return;
            }

            admin.displayRole();
            admin.createQuiz();
            adminMenu();
        }

        private void adminMenu() {
            while (true) {
                System.out.println("\n========== ADMIN MENU ==========");
                System.out.println("1. View All Questions");
                System.out.println("2. Add Question");
                System.out.println("3. Delete Question");
                System.out.println("4. Search Questions by Topic");
                System.out.println("5. View Questions by Difficulty");
                System.out.println("6. View All Student Results");
                System.out.println("7. Save Question Bank");
                System.out.println("8. Logout");

                int choice = readInt("Enter Choice : ");

                switch (choice) {
                    case 1 -> bank.showAll();
                    case 2 -> addQuestion();
                    case 3 -> deleteQuestion();
                    case 4 -> searchQuestions();
                    case 5 -> showDifficultyQuestions();
                    case 6 -> showAllResults();
                    case 7 -> bank.saveToFile(QUESTION_FILE);
                    case 8 -> {
                        System.out.println("Admin Logged Out.");
                        return;
                    }
                    default -> System.out.println("Invalid Choice.");
                }
            }
        }

        // =========================
        // ADD QUESTION
        // =========================

        private void addQuestion() {
            System.out.println("\n----- ADD QUESTION -----");

            String topic = readLine("Topic : ");
            String question = readLine("Question : ");
            String a = readLine("Option A : ");
            String b = readLine("Option B : ");
            String c = readLine("Option C : ");
            String d = readLine("Option D : ");

            String rawAnswer = readLine("Correct Answer (A/B/C/D) : ");

            if (rawAnswer.isBlank()) {
                System.out.println("Question Error: Correct answer must be A, B, C or D.");
                return;
            }

            char answer = Character.toUpperCase(rawAnswer.trim().charAt(0));

            Difficulty difficulty = readDifficulty();

            try {
                bank.addQuestion(topic, question, a, b, c, d,
                        answer, difficulty);

                System.out.println("Question Added Successfully.");
                bank.saveToFile(QUESTION_FILE);

            } catch (InvalidQuestionException e) {
                System.out.println("Question Error: " + e.getMessage());
            }
        }

        private void deleteQuestion() {
            int id = readInt("Enter Question ID to delete : ");

            if (bank.deleteQuestion(id)) {
                System.out.println("Question Deleted Successfully.");
                bank.saveToFile(QUESTION_FILE);
            } else {
                System.out.println("Question ID not found.");
            }
        }

        private void searchQuestions() {
            String topic = readLine("Enter Topic : ");
            bank.searchByTopic(topic);
        }

        private void showDifficultyQuestions() {
            Difficulty difficulty = readDifficulty();
            bank.showByDifficulty(difficulty);
        }

        // =========================
        // ADAPTIVE QUIZ
        // =========================

        private void takeAdaptiveQuiz() {

            if (bank.getQuestions().size() < 5) {
                System.out.println("At least 5 questions are required.");
                return;
            }

            currentAttempts.clear();

            System.out.println("\n==========================================");
            System.out.println("             ADAPTIVE QUIZ");
            System.out.println("==========================================");
            System.out.println("10 questions will be asked.");
            System.out.println("Each question has a 10-second time limit.");
            System.out.println("Correct -> difficulty increases.");
            System.out.println("Wrong/timeout -> difficulty decreases.");

            Difficulty currentDifficulty = Difficulty.EASY;
            Set<Integer> askedIds = new HashSet<>();

            int correct = 0;
            int total = Math.min(10, bank.getQuestions().size());

            for (int questionNumber = 1;
                 questionNumber <= total;
                 questionNumber++) {

                Question question =
                        chooseAdaptiveQuestion(currentDifficulty, askedIds);

                if (question == null) {
                    question = chooseAnyUnusedQuestion(askedIds);
                }

                if (question == null) {
                    break;
                }

                askedIds.add(question.id());

                System.out.println("\n------------------------------------------");
                System.out.println("Question " + questionNumber + " of " + total);
                System.out.println("Topic      : " + question.topic());
                System.out.println("Difficulty : " + question.difficulty());
                System.out.println("------------------------------------------");

                printQuestion(question);

                AnswerResult result =
                        readAnswerWithTimeout(10);

                if (result.timedOut()) {
                    System.out.println("\nTime Over!");
                    currentAttempts.add(new QuestionAttempt(question, false));
                    currentDifficulty = currentDifficulty.easier();

                } else {
                    char answer = result.answer();

                    if (answer == question.answer()) {
                        System.out.println("Correct!");
                        correct++;
                        currentAttempts.add(new QuestionAttempt(question, true));
                        currentDifficulty = currentDifficulty.harder();
                    } else {
                        System.out.println("Wrong!");
                        System.out.println("Correct Answer : " + question.answer());
                        currentAttempts.add(new QuestionAttempt(question, false));
                        currentDifficulty = currentDifficulty.easier();
                    }
                }

                System.out.println("Next Difficulty : " + currentDifficulty);
            }

            ResultCalculator calculator =
                    (right, totalQuestions) ->
                            totalQuestions == 0
                                    ? 0
                                    : (right * 100) / totalQuestions;

            int finalScore = calculator.calculate(correct, total);

            student.addScore(finalScore);
            saveResult(finalScore, correct, total);

            System.out.println("\n==========================================");
            System.out.println("              QUIZ FINISHED");
            System.out.println("==========================================");
            System.out.println("Correct Answers : " + correct);
            System.out.println("Total Questions : " + total);
            System.out.println("Score           : " + finalScore + "%");

            showPerformance(finalScore);
            new Analytics().showTopicPerformance(currentAttempts);
        }

        private Question chooseAdaptiveQuestion(
                Difficulty difficulty,
                Set<Integer> askedIds) {

            List<Question> matching =
                    bank.getQuestions()
                            .stream()
                            .filter(q -> q.difficulty() == difficulty)
                            .filter(q -> !askedIds.contains(q.id()))
                            .toList();

            if (matching.isEmpty()) {
                return null;
            }

            return matching.get(
                    new Random().nextInt(matching.size()));
        }

        private Question chooseAnyUnusedQuestion(Set<Integer> askedIds) {

            List<Question> unused =
                    bank.getQuestions()
                            .stream()
                            .filter(q -> !askedIds.contains(q.id()))
                            .toList();

            if (unused.isEmpty()) {
                return null;
            }

            return unused.get(
                    new Random().nextInt(unused.size()));
        }

        private void printQuestion(Question q) {
            System.out.println(q.text());
            System.out.println("A. " + q.A());
            System.out.println("B. " + q.B());
            System.out.println("C. " + q.C());
            System.out.println("D. " + q.D());
        }

        // =========================
        // TIMED INPUT
        // =========================

        /**
         * Waits up to `seconds` for an answer, pulling from the single
         * shared inputQueue (fed by the one background reader thread).
         *
         * The queue is drained right before waiting so that any answer the
         * student typed too late for a PREVIOUS question (after that
         * question already timed out) does not leak in and get silently
         * counted as this question's answer.
         */
        private AnswerResult readAnswerWithTimeout(int seconds) {
            inputQueue.clear();

            System.out.print("Answer (" + seconds + "s) : ");

            try {
                String input = inputQueue.poll(seconds, TimeUnit.SECONDS);

                if (input == null) {
                    return new AnswerResult(' ', true);
                }

                input = input.trim();

                if (input.isEmpty()) {
                    return new AnswerResult(' ', false);
                }

                return new AnswerResult(
                        Character.toUpperCase(input.charAt(0)),
                        false);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new AnswerResult(' ', true);
            }
        }

        record AnswerResult(char answer, boolean timedOut) {
        }

        // =========================
        // PERFORMANCE
        // =========================

        private void showPreviousScores() {
            System.out.println("\n----- PREVIOUS SCORES -----");

            if (student.getScores().isEmpty()) {
                System.out.println("No scores available.");
                return;
            }

            student.getScores()
                    .stream()
                    .forEach(score ->
                            System.out.println("Score : " + score + "%"));

            double average =
                    student.getScores()
                            .stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0);

            System.out.printf("Average : %.2f%%%n", average);
        }

        private void performance() {
            new Analytics().new Report().display();

            showPreviousScores();

            if (!student.getScores().isEmpty()) {
                int latest =
                        student.getScores()
                                .get(student.getScores().size() - 1);

                showPerformance(latest);
            }
        }

        private void showPerformance(int score) {
            String performance;

            if (score >= 80) {
                performance = "Excellent";
            } else if (score >= 60) {
                performance = "Good";
            } else if (score >= 40) {
                performance = "Average";
            } else {
                performance = "Needs Improvement";
            }

            System.out.println("Performance : " + performance);
        }

        private void cloneStudent() {
            Student copy = student.clone();

            System.out.println("\nStudent profile cloned successfully.");
            System.out.println("Original Username : " + student.getUsername());
            System.out.println("Cloned Username   : " + copy.getUsername());
            System.out.println("Cloned Scores     : " + copy.getScores());
        }

        // =========================
        // FILE HANDLING
        // =========================

        private void saveResult(int score, int correct, int total) {

            boolean fileExists =
                    new File(RESULT_FILE).exists();

            try (PrintWriter out =
                         new PrintWriter(
                                 new FileWriter(RESULT_FILE, true))) {

                if (!fileExists) {
                    out.println("Username,DateTime,Correct,Total,Score");
                }

                String date =
                        LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern(
                                        "yyyy-MM-dd HH:mm:ss"));

                out.println(student.getUsername() + "," +
                        date + "," +
                        correct + "," +
                        total + "," +
                        score);

                System.out.println("Result Saved Successfully.");

            } catch (IOException e) {
                System.out.println("Could not save result: " + e.getMessage());
            }
        }

        private void loadExistingResults() {
            File file = new File(RESULT_FILE);

            if (!file.exists()) {
                return;
            }

            try (BufferedReader reader =
                         new BufferedReader(new FileReader(file))) {

                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {

                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    String[] parts = line.split(",");

                    if (parts.length >= 5 &&
                            parts[0].equals(student.getUsername())) {

                        try {
                            int score = Integer.parseInt(parts[4].trim());
                            student.addScore(score);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("Could not load previous results.");
            }
        }

        private void showAllResults() {
            File file = new File(RESULT_FILE);

            if (!file.exists()) {
                System.out.println("No result file found.");
                return;
            }

            System.out.println("\n----- ALL STUDENT RESULTS -----");

            try (BufferedReader reader =
                         new BufferedReader(new FileReader(file))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

            } catch (IOException e) {
                System.out.println("Could not read results.");
            }
        }

        // =========================
        // INPUT HELPERS
        // =========================

        /**
         * Blocking read of one line from the shared queue - used for every
         * non-timed prompt (menus, logins, add-question fields, etc).
         */
        private String readLine(String message) {
            System.out.print(message);
            try {
                String line = inputQueue.take();
                return line.trim();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }

        private int readInt(String message) {
            while (true) {
                try {
                    return Integer.parseInt(readLine(message));
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.");
                }
            }
        }

        private Difficulty readDifficulty() {
            while (true) {
                String input =
                        readLine("Difficulty (1=Easy, 2=Medium, 3=Hard) : ");

                switch (input) {
                    case "1" -> {
                        return Difficulty.EASY;
                    }
                    case "2" -> {
                        return Difficulty.MEDIUM;
                    }
                    case "3" -> {
                        return Difficulty.HARD;
                    }
                    default -> System.out.println("Invalid difficulty.");
                }
            }
        }
    }
}