package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Integer>{

    // Find matches by status - CORRECTED METHOD NAME
    List<Match> findByStatus(String status);

    // Find upcoming matches
    @Query("SELECT m FROM Match m WHERE m.matchDate > :currentTime ORDER BY m.matchDate ASC")
    List<Match> findUpcomingMatches(LocalDateTime currentTime);

    // Find past matches
    @Query("SELECT m FROM Match m WHERE m.matchDate < :currentTime ORDER BY m.matchDate DESC")
    List<Match> findPastMatches(LocalDateTime currentTime);

    // Find matches by team (home or away)
    @Query("SELECT m FROM Match m WHERE m.homeTeam = :team OR m.awayTeam = :team ORDER BY m.matchDate DESC")
    List<Match> findByTeam(Team team);

    // Find matches by league
    List<Match> findByLeague(String league);

    // Find matches between two teams
    @Query("SELECT m FROM Match m WHERE (m.homeTeam = :team1 AND m.awayTeam = :team2) OR (m.homeTeam = :team2 AND m.awayTeam = :team1) ORDER BY m.matchDate DESC")
    List<Match> findMatchesBetweenTeams(Team team1, Team team2);

    // Find matches by date range
    @Query("SELECT m FROM Match m WHERE m.matchDate BETWEEN :startDate AND :endDate ORDER BY m.matchDate ASC")
    List<Match> findByMatchDateBetween(LocalDateTime startDate, LocalDateTime endDate);

}
