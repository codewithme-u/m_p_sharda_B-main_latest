package com.mp.service;

import com.mp.entity.Quiz;
import com.mp.entity.User;
import com.mp.repository.QuizRepository;
import com.mp.repository.UserRepository; // ✅ Import UserRepository
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class QuizService {
    private final QuizRepository repository;
    private final UserRepository userRepository; // ✅ Inject UserRepository

    public QuizService(QuizRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // ✅ UPDATED: Get only quizzes for the logged-in user
    public List<Quiz> getQuizzesByUser(String email) {
        return repository.findByCreatedByEmail(email);
    }

    // ✅ UPDATED: Save quiz with the creator
    public Quiz createQuiz(String title, String description, String userEmail) {
        // Find the user who is creating this
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String uniqueCode = Long.toHexString(Double.doubleToLongBits(Math.random())).substring(0, 6).toUpperCase();
        
        // Pass creator to constructor
        Quiz quiz = new Quiz(title, description, uniqueCode, creator);
        return repository.save(quiz);
    }

    public void deleteQuiz(Long id) {
        repository.deleteById(id);
    }
}