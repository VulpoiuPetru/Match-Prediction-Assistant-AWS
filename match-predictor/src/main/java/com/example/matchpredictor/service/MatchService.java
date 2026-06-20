package com.example.matchpredictor.service;

import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.repository.MatchRepository;
import com.example.matchpredictor.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private TeamService teamService;

    //Get all matches
    public List<Match> getAllMatches(){
        return matchRepository.findAll();
    }

    //Get match by ID
    public Optional<Match> getMatchById(Integer id) {
        return matchRepository.findById(id);
    }

    // Get upcoming matches
    public List<Match> getUpcomingMatches() {
        return matchRepository.findUpcomingMatches(LocalDateTime.now());
    }

    // Get past matches
    public List<Match> getPastMatches() {
        return matchRepository.findPastMatches(LocalDateTime.now());
    }

    // Get matches by status
    public List<Match> getMatchesByStatus(String status) {
        return matchRepository.findByStatus(status);
    }

    // Get matches by team
    public List<Match> getMatchesByTeam(Integer teamId) {
        Team team = teamService.getTeamById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));
        return matchRepository.findByTeam(team);
    }

    //Get matches by league
    public List<Match> getMatchesByLeague(String league) {
        return matchRepository.findByLeague(league);
    }

    // Create new match
    public Match createMatch(Match match) {
        // Validate teams exist
        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new RuntimeException("Home team and away team are required");
        }

        // Validate teams are different
        if (match.getHomeTeam().getId().equals(match.getAwayTeam().getId())) {
            throw new RuntimeException("Home team and away team must be different");
        }

        // Set default status if not provided
        if (match.getStatus() == null) {
            match.setStatus("SCHEDULED");
        }

        return matchRepository.save(match);
    }

    // Update match
    public Match updateMatch(Integer id, Match matchDetails) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        match.setHomeTeam(matchDetails.getHomeTeam());
        match.setAwayTeam(matchDetails.getAwayTeam());
        match.setMatchDate(matchDetails.getMatchDate());
        match.setLeague(matchDetails.getLeague());
        match.setVenue(matchDetails.getVenue());
        match.setStatus(matchDetails.getStatus());
        match.setHomeScore(matchDetails.getHomeScore());
        match.setAwayScore(matchDetails.getAwayScore());

        return matchRepository.save(match);
    }

    // Update match result
    public Match updateMatchResult(Integer id, Integer homeScore, Integer awayScore) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setStatus("FINISHED");

        return matchRepository.save(match);
    }

    // Delete match
    public void deleteMatch(Integer id) {
        if (!matchRepository.existsById(id)) {
            throw new RuntimeException("Match not found with id: " + id);
        }
        matchRepository.deleteById(id);
    }

    // Get match count
    public long getMatchCount() {
        return matchRepository.count();
    }
}
