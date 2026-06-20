package com.example.matchpredictor.dto;

import jakarta.validation.constraints.NotNull;

public class ContextualPredictionRequest {
    @NotNull(message = "Match ID is required")
    private Integer matchId;

    private String sessionId; // For conversation memory
    private String tone;  // "professional", "casual", "detailed", "brief"
    private String role;  //admin,user
    private String additionalContext; //user specific question or focus

    // Constructors
    public ContextualPredictionRequest() {}

    public ContextualPredictionRequest(Integer matchId) {
        this. matchId = matchId;
    }

    // Getters and Setters
    public Integer getMatchId() { return matchId; }
    public void setMatchId(Integer matchId) { this.matchId = matchId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
}
