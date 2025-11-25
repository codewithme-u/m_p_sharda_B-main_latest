package com.mp.controller;

import com.mp.entity.Quiz;
import com.mp.entity.User;
import com.mp.repository.QuizRepository;
import com.mp.repository.UserRepository;
import com.mp.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    
    private final QuizService service;
    private final QuizRepository repository;
    private final UserRepository userRepository;

    public QuizController(QuizService service, QuizRepository repository, UserRepository userRepository) {
        this.service = service;
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Quiz>> getMyQuizzes(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(service.getQuizzesByUser(principal.getName()));
    }

    /**
     * FINAL, COMPLETE SECURITY CHECK FOR QUIZ ACCESS:
     * Enforces three rules:
     * 1. If Quiz is created by a Faculty (has Institute), Student MUST have the same Institute.
     * 2. If Quiz is General-created, blocks Institute Users (to ensure separation).
     * 3. Blocks cross-institutional access.
     *
     * Also blocks access immediately if the quiz is deactivated and returns a structured body.
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<?> getByCode(@PathVariable String code, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("message", "Session expired. Please login again."));

        Optional<Quiz> quizOpt = repository.findByCode(code);
        if (quizOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Quiz not found. Check the code."));

        Quiz quiz = quizOpt.get();

        // Block if quiz is inactive/deactivated
        // NOTE: Quiz.active is a primitive boolean -> Lombok generates isActive()
        if (!quiz.isActive()) {
            return ResponseEntity.status(403).body(Map.of(
                    "reason", "DEACTIVATED",
                    "message", "This quiz has been deactivated by the instructor."
            ));
        }

        User student = userRepository.findByEmail(principal.getName()).orElse(null);
        if (student == null) return ResponseEntity.status(401).body(Map.of("message", "User not found"));

        String quizCreatorType = quiz.getCreatedBy().getUserType();
        String playerUserType = student.getUserType();

        // --- Rule 1 & 2: Enforce Separation by User Type ---
        if (!quizCreatorType.equals(playerUserType)) {
             if ("GENERAL".equals(quizCreatorType)) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Access Denied: This quiz is for General Users only."));
            }
            if ("INSTITUTE".equals(quizCreatorType)) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Access Denied: This quiz is restricted to institutional students."));
            }
        }

        // --- Rule 3: Enforce Institutional Lock (Only for Institute Quizzes) ---
        if ("INSTITUTE".equals(quizCreatorType)) {
            if (student.getInstitution() == null) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Access Denied: You must be linked to an institution to play."));
            }

            Long studentInstId = student.getInstitution().getId();
            Long quizInstId = quiz.getCreatedBy().getInstitution().getId();

            if (!studentInstId.equals(quizInstId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Access Denied: You must be a student of " + quiz.getCreatedBy().getInstitution().getInstituteName() + " to play this."));
            }
        }

        // Access Allowed
        return ResponseEntity.ok(quiz);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> payload, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        String title = payload.get("title");
        String description = payload.get("description");
        return ResponseEntity.ok(service.createQuiz(title, description, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.deleteQuiz(id);
        return ResponseEntity.ok().build();
    }

    
    /**
     * New endpoint: update quiz active status (Activate / Deactivate)
     * Expects JSON: { "active": true | false }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        Optional<Quiz> quizOpt = repository.findById(id);
        if (quizOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("message", "Quiz not found"));

        Quiz quiz = quizOpt.get();

        // Optionally enforce permission: only creator or admin can toggle
        if (principal == null || !quiz.getCreatedBy().getEmail().equals(principal.getName())) {
            // You can also allow admins - adjust as per your authorization logic
            // For now require creator
            return ResponseEntity.status(403).body(Map.of("message", "Not authorized to change quiz status"));
        }

        Boolean active = null;
        if (body.containsKey("active")) {
            Object v = body.get("active");
            if (v instanceof Boolean) active = (Boolean) v;
            else if (v instanceof String) active = Boolean.parseBoolean((String) v);
        }

        if (active == null) return ResponseEntity.badRequest().body(Map.of("message", "Missing 'active' boolean in body"));

        // Unbox Boolean to primitive boolean (Quiz.active is primitive)
        quiz.setActive(active);
        repository.save(quiz);

        return ResponseEntity.ok(quiz);
    }
}
