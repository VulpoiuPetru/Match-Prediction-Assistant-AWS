package com.example.matchpredictor.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Home team is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @NotNull(message = "Away team is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    @NotNull(message = "Match date is required")
    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate;

    @NotBlank(message = "League is required")
    @Size(max=100, message = "League name cannot exceed 100 characters")
    @Column(nullable = false,length = 100)
    private String league;

    @Size(max = 100, message = "Venue name cannot exceed 100 characters")
    @Column(length = 100)
    private String venue;

    @Column(length = 20)
    private String status = "SCHEDULED";

    @Column(name = "home_score")
    private Integer homeScore = 0;

    @Column(name = "away_score")
    private Integer awayScore = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    //One-toMany relationship with AiPrediction
    @JsonIgnore
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AiPrediction> aiPredictions;

    // One-to-Many relationship with UserPrediction
    @JsonIgnore
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserPrediction> userPredictions;

    // Constructors
    public Match() {}

    public Match(Team homeTeam, Team awayTeam, LocalDateTime matchDate, String league) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.matchDate = matchDate;
        this.league = league;
    }

    //Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Team getHomeTeam() { return homeTeam; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }

    public Team getAwayTeam() { return awayTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }

    public LocalDateTime getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDateTime matchDate) { this.matchDate = matchDate; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<AiPrediction> getAiPredictions() { return aiPredictions; }
    public void setAiPredictions(List<AiPrediction> aiPredictions) { this.aiPredictions = aiPredictions; }

    public List<UserPrediction> getUserPredictions() { return userPredictions; }
    public void setUserPredictions(List<UserPrediction> userPredictions) { this.userPredictions = userPredictions; }



}
