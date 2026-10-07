/* =====================================================
   QUIZNOVA DASHBOARD
   Login + Student/Admin + Adaptive Quiz
   ===================================================== */


/* ================= USER DATA ================= */

const users = {
    student: {
        username: "smriti",
        password: "123",
        role: "student"
    },

    admin: {
        username: "admin",
        password: "admin",
        role: "admin"
    }
};

let currentUser = null;


/* ================= QUIZ DATA ================= */

const questions = [

    {
        topic: "Java",
        difficulty: "EASY",
        question: "What is the default value of an int variable in Java?",
        options: ["0", "1", "null", "-1"],
        answer: 0
    },

    {
        topic: "OOP",
        difficulty: "EASY",
        question: "Which concept allows a class to acquire properties of another class?",
        options: ["Encapsulation", "Inheritance", "Polymorphism", "Abstraction"],
        answer: 1
    },

    {
        topic: "Collections",
        difficulty: "MEDIUM",
        question: "Which interface represents an ordered collection?",
        options: ["Set", "Map", "List", "Queue"],
        answer: 2
    },

    {
        topic: "Exceptions",
        difficulty: "MEDIUM",
        question: "Which keyword is used to handle an exception?",
        options: ["try", "final", "static", "extends"],
        answer: 0
    },

    {
        topic: "Java",
        difficulty: "HARD",
        question: "Which keyword prevents a method from being overridden?",
        options: ["static", "final", "private", "const"],
        answer: 1
    },

    {
        topic: "OOP",
        difficulty: "HARD",
        question: "Which feature allows the same method name with different parameters?",
        options: ["Overloading", "Overriding", "Inheritance", "Abstraction"],
        answer: 0
    }

];


/* ================= QUIZ VARIABLES ================= */

let quizQuestions = [];
let currentQuestion = 0;
let selectedAnswer = null;
let correctAnswers = 0;
let currentDifficulty = "EASY";
let timer;
let timeLeft = 10;


/* ================= LOGIN ================= */

function login() {

    const username =
        document.getElementById("username").value.trim().toLowerCase();

    const password =
        document.getElementById("password").value;

    const message =
        document.getElementById("loginMessage");


    let matchedUser = null;


    if (
        username === users.student.username &&
        password === users.student.password
    ) {
        matchedUser = users.student;
    }

    else if (
        username === users.admin.username &&
        password === users.admin.password
    ) {
        matchedUser = users.admin;
    }


    if (!matchedUser) {

        message.textContent =
            "Invalid username or password.";

        return;
    }


    currentUser = matchedUser;

    document.getElementById("loginPage")
        .classList.add("hidden");

    document.getElementById("app")
        .classList.remove("hidden");


    setupUserInterface();
}


/* ================= USER INTERFACE ================= */

function setupUserInterface() {

    const isStudent =
        currentUser.role === "student";


    document.getElementById("sideUsername")
        .textContent = currentUser.username;

    document.getElementById("sideRole")
        .textContent =
        isStudent ? "Student" : "Administrator";

    document.getElementById("sideAvatar")
        .textContent =
        currentUser.username.charAt(0).toUpperCase();

    document.getElementById("welcomeTitle")
        .textContent =
        isStudent
            ? "Welcome, Smriti 👋"
            : "Welcome, Admin 👋";

    document.getElementById("roleBadge")
        .textContent =
        isStudent ? "STUDENT" : "ADMINISTRATOR";


    document.querySelectorAll(".student-only")
        .forEach(item => {

            item.style.display =
                isStudent ? "block" : "none";

        });


    document.querySelectorAll(".admin-only")
        .forEach(item => {

            item.style.display =
                isStudent ? "none" : "block";

        });


    document.getElementById("studentDashboard")
        .classList.toggle("hidden", !isStudent);

    document.getElementById("adminDashboard")
        .classList.toggle("hidden", isStudent);


    showSectionById("dashboard");
}


/* ================= NAVIGATION ================= */

function showSection(sectionId, button) {

    document.querySelectorAll(".section")
        .forEach(section => {

            section.classList.remove("active-section");

        });


    document.getElementById(sectionId)
        .classList.add("active-section");


    document.querySelectorAll(".nav-item")
        .forEach(item => {

            item.classList.remove("active");

        });


    if (button) {
        button.classList.add("active");
    }

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


function showSectionById(sectionId) {

    document.querySelectorAll(".section")
        .forEach(section => {

            section.classList.remove("active-section");

        });


    document.getElementById(sectionId)
        .classList.add("active-section");


    document.querySelectorAll(".nav-item")
        .forEach(item => {

            item.classList.remove("active");

        });

}


/* ================= LOGOUT ================= */

function logout() {

    clearInterval(timer);

    currentUser = null;

    document.getElementById("app")
        .classList.add("hidden");

    document.getElementById("loginPage")
        .classList.remove("hidden");

    document.getElementById("username")
        .value = "";

    document.getElementById("password")
        .value = "";

    document.getElementById("loginMessage")
        .textContent = "";
}


/* ================= START QUIZ ================= */

function openQuizFromDashboard() {

    showSectionById("quiz");

    startQuiz();
}


function startQuiz() {

    quizQuestions = [];

    currentQuestion = 0;

    correctAnswers = 0;

    currentDifficulty = "EASY";


    /*
       Start with EASY questions.
       After each answer the difficulty changes.
    */

    quizQuestions = questions.slice();


    document.getElementById("difficultyText")
        .textContent = currentDifficulty;


    loadQuestion();
}


/* ================= LOAD QUESTION ================= */

function loadQuestion() {

    clearInterval(timer);

    selectedAnswer = null;

    timeLeft = 10;


    if (currentQuestion >= 10) {

        finishQuiz();

        return;
    }


    /*
       Find a question matching
       the current adaptive difficulty.
    */

    let matchingQuestions =
        quizQuestions.filter(q =>
            q.difficulty === currentDifficulty
        );


    /*
       If there is no question at the
       required difficulty, use any question.
    */

    if (matchingQuestions.length === 0) {

        matchingQuestions = quizQuestions;

    }


    const question =
        matchingQuestions[
            currentQuestion %
            matchingQuestions.length
        ];


    document.getElementById("questionNumber")
        .textContent =
        "Question " + (currentQuestion + 1) + " of 10";


    document.getElementById("topicLabel")
        .textContent = question.topic;


    document.getElementById("questionText")
        .textContent = question.question;


    document.getElementById("difficultyText")
        .textContent = currentDifficulty;


    const optionsContainer =
        document.getElementById("optionsContainer");


    optionsContainer.innerHTML = "";


    question.options.forEach((option, index) => {

        const button =
            document.createElement("button");

        button.className = "option";

        button.textContent =
            String.fromCharCode(65 + index)
            + ". "
            + option;


        button.onclick = function() {

            selectOption(this, index);

        };


        optionsContainer.appendChild(button);

    });


    document.getElementById("quizFeedback")
        .textContent = "";


    startTimer();
}


/* ================= SELECT OPTION ================= */

function selectOption(button, index) {

    document.querySelectorAll(".option")
        .forEach(option => {

            option.classList.remove("selected");

        });


    button.classList.add("selected");

    selectedAnswer = index;
}


/* ================= TIMER ================= */

function startTimer() {

    document.getElementById("timer")
        .textContent = timeLeft;


    timer = setInterval(() => {

        timeLeft--;

        document.getElementById("timer")
            .textContent = timeLeft;


        if (timeLeft <= 0) {

            clearInterval(timer);

            timeoutAnswer();

        }

    }, 1000);
}


/* ================= SUBMIT ================= */

function submitAnswer() {

    if (selectedAnswer === null) {

        document.getElementById("quizFeedback")
            .textContent =
            "Please select an answer.";

        return;
    }


    clearInterval(timer);


    processAnswer(selectedAnswer);
}


/* ================= PROCESS ANSWER ================= */

function processAnswer(answer) {

    const question =
        getCurrentQuestion();


    if (answer === question.answer) {

        correctAnswers++;


        document.getElementById("quizFeedback")
            .textContent =
            "✓ Correct! Difficulty increased.";


        increaseDifficulty();

    }

    else {

        document.getElementById("quizFeedback")
            .textContent =
            "✗ Incorrect. Difficulty decreased.";


        decreaseDifficulty();

    }


    updateDifficultyDisplay();


    setTimeout(() => {

        currentQuestion++;

        loadQuestion();

    }, 1000);
}


/* ================= TIMEOUT ================= */

function timeoutAnswer() {

    document.getElementById("quizFeedback")
        .textContent =
        "⏰ Time's up! Difficulty decreased.";


    decreaseDifficulty();

    updateDifficultyDisplay();


    setTimeout(() => {

        currentQuestion++;

        loadQuestion();

    }, 1000);
}


/* ================= CURRENT QUESTION ================= */

function getCurrentQuestion() {

    let matchingQuestions =
        quizQuestions.filter(q =>
            q.difficulty === currentDifficulty
        );


    if (matchingQuestions.length === 0) {

        matchingQuestions = quizQuestions;

    }


    return matchingQuestions[
        currentQuestion %
        matchingQuestions.length
    ];
}


/* ================= ADAPTIVE LOGIC ================= */

function increaseDifficulty() {

    if (currentDifficulty === "EASY") {

        currentDifficulty = "MEDIUM";

    }

    else if (currentDifficulty === "MEDIUM") {

        currentDifficulty = "HARD";

    }

}


function decreaseDifficulty() {

    if (currentDifficulty === "HARD") {

        currentDifficulty = "MEDIUM";

    }

    else if (currentDifficulty === "MEDIUM") {

        currentDifficulty = "EASY";

    }

}


function updateDifficultyDisplay() {

    document.getElementById("difficultyText")
        .textContent = currentDifficulty;


    document.getElementById("currentDifficultyCard")
        .textContent = currentDifficulty;

}


/* ================= FINISH QUIZ ================= */

function finishQuiz() {

    clearInterval(timer);


    const score =
        Math.round(
            (correctAnswers / 10) * 100
        );


    document.getElementById("quizFeedback")
        .textContent =
        "Quiz completed! Score: "
        + score
        + "%";


    alert(
        "Quiz Completed!\n\n"
        + "Correct Answers: "
        + correctAnswers
        + "/10\n"
        + "Score: "
        + score
        + "%"
    );


    showSectionById("results");

}


/* ================= ADMIN DEMO ================= */

function addDemoQuestion() {

    const table =
        document.getElementById("questionBankTable");


    const row =
        table.insertRow();


    row.innerHTML = `
        <td>Java</td>
        <td>What is JVM?</td>
        <td><span class="easy">EASY</span></td>
    `;


    alert(
        "Question added to the dashboard demo."
    );
}