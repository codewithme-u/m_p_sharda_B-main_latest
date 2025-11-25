package com.mp.repository;

import com.mp.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // ✅ EXISTING: Find by unique code (for playing)
    Optional<Quiz> findByCode(String code);

    // ✅ NEW: Find quizzes created by a specific user (for dashboard)
    List<Quiz> findByCreatedByEmail(String email);
}