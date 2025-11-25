package com.mp.controller;

import com.mp.entity.Question;
import com.mp.entity.Quiz;
import com.mp.entity.QuizResult;
import com.mp.entity.User;
import com.mp.repository.QuestionRepository;
import com.mp.repository.QuizRepository;
import com.mp.repository.QuizResultRepository;
import com.mp.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/results")
public class QuizResultController {

    private final QuizResultRepository resultRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    public QuizResultController(QuizResultRepository resultRepository, QuizRepository quizRepository, UserRepository userRepository, QuestionRepository questionRepository) {
        this.resultRepository = resultRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
    }

    // ============================================================
    // 1. SUBMIT QUIZ (Student)
    // ============================================================
    @PostMapping("/submit/{quizCode}")
    public ResponseEntity<?> submitQuiz(@PathVariable String quizCode, @RequestBody Map<Long, String> studentAnswers, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        User student = userRepository.findByEmail(principal.getName()).orElse(null);
        Quiz quiz = quizRepository.findByCode(quizCode).orElse(null);

        if (student == null || quiz == null) return ResponseEntity.badRequest().body("Invalid Request");

        // Calculate Score
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        int score = 0;
        
        for (Question q : questions) {
            if ("MCQ".equals(q.getType())) {
                String selected = studentAnswers.get(q.getId());
                if (selected != null && selected.equals(q.getCorrectAnswer())) {
                    score++;
                }
            }
        }

        QuizResult result = new QuizResult();
        result.setUser(student);
        result.setQuiz(quiz);
        result.setScore(score);
        result.setTotalQuestions(questions.size());
        // Pass if >= 50%
        result.setStatus(score >= (questions.size() / 2.0) ? "Pass" : "Fail"); 
        result.setAttemptDate(LocalDate.now());
        result.setAnswers(studentAnswers);

        resultRepository.save(result);
        return ResponseEntity.ok("Quiz Submitted. Score: " + score);
    }

    // ============================================================
    // 2. GET MY HISTORY (Student Dashboard)
    // ============================================================
    @GetMapping("/history")
    public ResponseEntity<List<HistoryDTO>> getMyHistory(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        List<QuizResult> results = resultRepository.findByUserEmailOrderByAttemptDateDesc(principal.getName());

        List<HistoryDTO> dtos = results.stream().map(r -> new HistoryDTO(
                r.getId(),
                r.getQuiz().getTitle(),
                r.getQuiz().getCode(),
                r.getScore(),
                r.getTotalQuestions(),
                r.getAttemptDate().toString(),
                r.getStatus()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ============================================================
    // 3. GET PARTICIPANTS (Teacher Dashboard)
    // ============================================================
    @GetMapping("/participants/{quizId}")
    public ResponseEntity<List<ParticipantDTO>> getParticipants(@PathVariable Long quizId) {
        List<QuizResult> results = resultRepository.findByQuizIdOrderByScoreDesc(quizId);

        List<ParticipantDTO> dtos = results.stream().map(r -> new ParticipantDTO(
                r.getUser().getName(),
                r.getUser().getEmail(),
                r.getScore(),
                r.getTotalQuestions(),
                r.getAttemptDate().toString()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ============================================================
    // 4. GET REVIEW DETAILS (Review Modal)
    // ============================================================
    @GetMapping("/review/{resultId}")
    public ResponseEntity<?> getReviewDetails(@PathVariable Long resultId, Principal principal) {
        QuizResult result = resultRepository.findById(resultId).orElse(null);
        if (result == null) return ResponseEntity.notFound().build();

        // Security: Only the student who took the quiz can see their review
        if (!result.getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access Denied");
        }

        List<Question> questions = questionRepository.findByQuizId(result.getQuiz().getId());

        ReviewDTO dto = new ReviewDTO();
        dto.quizTitle = result.getQuiz().getTitle();
        dto.quizCode = result.getQuiz().getCode();
        dto.facultyName = result.getQuiz().getCreatedBy().getName();
        dto.facultyEmail = result.getQuiz().getCreatedBy().getEmail();
        dto.score = result.getScore();
        dto.totalQuestions = result.getTotalQuestions();
        dto.userAnswers = result.getAnswers();
        dto.questions = questions;

        return ResponseEntity.ok(dto);
    }

    // ============================================================
    // DTOs
    // ============================================================

    public static class HistoryDTO {
        public Long id;
        public String quizTitle;
        public String quizCode;
        public int score;
        public int totalQuestions;
        public String dateAttempted;
        public String status;

        public HistoryDTO(Long id, String t, String c, int s, int tq, String d, String st) {
            this.id=id; quizTitle=t; quizCode=c; score=s; totalQuestions=tq; dateAttempted=d; status=st;
        }
    }

    public static class ParticipantDTO {
        public String name;
        public String email;
        public int score;
        public int totalQuestions;
        public String date;

        public ParticipantDTO(String name, String email, int score, int totalQuestions, String date) {
            this.name = name;
            this.email = email;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.date = date;
        }
    }

    public static class ReviewDTO {
        public String quizTitle;
        public String quizCode;
        public String facultyName;
        public String facultyEmail;
        public int score;
        public int totalQuestions;
        public Map<Long, String> userAnswers;
        public List<Question> questions;
    }
}