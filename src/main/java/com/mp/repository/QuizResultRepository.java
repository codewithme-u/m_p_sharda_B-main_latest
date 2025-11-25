package com.mp.repository;

import com.mp.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    // For Student History
    List<QuizResult> findByUserEmailOrderByAttemptDateDesc(String email);
    
    // ✅ NEW: For Creator Analytics (Who played a specific quiz?)
    List<QuizResult> findByQuizIdOrderByScoreDesc(Long quizId);
}