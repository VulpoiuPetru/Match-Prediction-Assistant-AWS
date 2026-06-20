package com.example.matchpredictor.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name cannot exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Country is required")
    @Size(max = 50, message = "Country name cannot exceed 50 chracters")
    @Column(nullable = false, length = 50)
    private String country;

    @Column(length = 255)
    private String logo;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    //One-to-Many relationship with Match
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "homeTeam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Match> homeMatches;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "awayTeam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Match> awayMatches;

    // Constructors
    public Team() {}

    public Team(String name, String country) {
        this.name = name;
        this.country = country;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public Integer getFoundedYear() { return foundedYear; }
    public void setFoundedYear(Integer foundedYear) { this.foundedYear = foundedYear; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Match> getHomeMatches() { return homeMatches; }
    public void setHomeMatches(List<Match> homeMatches) { this.homeMatches = homeMatches; }

    public List<Match> getAwayMatches() { return awayMatches; }
    public void setAwayMatches(List<Match> awayMatches) { this.awayMatches = awayMatches; }

}
