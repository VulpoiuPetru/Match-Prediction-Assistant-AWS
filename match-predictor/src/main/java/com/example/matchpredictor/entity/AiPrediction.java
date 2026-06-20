package com.example.matchpredictor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_predictions")
public class AiPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Match is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @NotNull(message = "Home win probability is required")
    @DecimalMin(value = "0.00", message = "Home win probability must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Home win probability must not exceed 100.00")
    @Column(name = "home_win_probability", precision = 5, scale = 2, nullable = false)
    private BigDecimal homeWinProbability;

    @NotNull(message = "Draw probability is required")
    @DecimalMin(value = "0.00", message = "Draw probability must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Draw probability must not exceed 100.00")
    @Column(name = "draw_probability", precision = 5, scale = 2, nullable = false)
    private BigDecimal drawProbability;

    @NotNull(message = "Away win probability is required")
    @DecimalMin(value = "0.00", message = "Away win probability must be at least 0.00")
    @DecimalMax(value = "100.00", message = "Away win probability must not exceed 100.00")
    @Column(name = "away_win_probability", precision = 5, scale = 2, nullable = false)
    private BigDecimal awayWinProbability;

    @NotBlank(message = "Reasoning is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reasoning;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @DecimalMin(value = "0.00", message = "Confidence score must be at least 0.00")
    @DecimalMax(value = "1.00", message = "Confidence score must not exceed 1.00")
    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public AiPrediction() {}

    public AiPrediction(Match match, BigDecimal homeWinProbability, BigDecimal drawProbability,
                        BigDecimal awayWinProbability, String reasoning) {
        this.match = match;
        this.homeWinProbability = homeWinProbability;
        this.drawProbability = drawProbability;
        this.awayWinProbability = awayWinProbability;
        this.reasoning = reasoning;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public BigDecimal getHomeWinProbability() { return homeWinProbability; }
    public void setHomeWinProbability(BigDecimal homeWinProbability) { this.homeWinProbability = homeWinProbability; }

    public BigDecimal getDrawProbability() { return drawProbability; }
    public void setDrawProbability(BigDecimal drawProbability) { this.drawProbability = drawProbability; }

    public BigDecimal getAwayWinProbability() { return awayWinProbability; }
    public void setAwayWinProbability(BigDecimal awayWinProbability) { this.awayWinProbability = awayWinProbability; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
