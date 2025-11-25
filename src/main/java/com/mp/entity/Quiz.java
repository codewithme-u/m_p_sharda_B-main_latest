package com.mp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "quizzes")
@Data // Includes Getters and Setters
@NoArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    
    @Column(columnDefinition = "TEXT") 
    private String description;
    
    @Column(unique = true)
    private String code;
    
    private boolean active;
    private int questionsCount;
    private LocalDate createdDate;

    // FIX: Only show essential User info to break the loop
    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "FK_QUIZ_CREATOR_REF")) // ✅ Added FK Name
    @JsonIgnoreProperties({"password", "roles", "institution", "hibernateLazyInitializer", "handler"})
    private User createdBy;

    // Custom constructor
    public Quiz(String title, String description, String code, User createdBy) {
        this.title = title;
        this.description = description;
        this.code = code;
        this.createdBy = createdBy;
        this.active = true;
        this.questionsCount = 0;
        this.createdDate = LocalDate.now();
    }
}