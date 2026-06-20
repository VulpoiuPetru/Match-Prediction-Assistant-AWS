package com.example.matchpredictor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_predictions")
public class UserPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Prompt is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @NotBlank(message = "Response is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "embedding_data", columnDefinition = "TEXT")
    private String embeddingData;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(name = "session_id")
    private String sessionId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public UserPrediction() {}

    public UserPrediction(User user, String prompt, String response) {
        this.user = user;
        this.prompt = prompt;
        this.response = response;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getEmbeddingData() { return embeddingData; }
    public void setEmbeddingData(String embeddingData) { this.embeddingData = embeddingData; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

}
