package com.example.matchpredictor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_contexts")
public class ConversationContext {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Integer id;

    @NotBlank(message = "Session ID is required")
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @NotBlank(message = "User message is required")
    @Column(name = "user_message", nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(length = 50)
    private String tone;

    @Column(length = 50)
    private String role;

    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public ConversationContext() {}

    public ConversationContext(String sessionId, String userMessage, String tone, String role) {
        this. sessionId = sessionId;
        this. userMessage = userMessage;
        this. tone = tone;
        this.role = role;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
