package com.example.matchpredictor.controller;

import com.example.matchpredictor.dto.TeamPredictionRequest;
import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.service.AiPredictionService;
import com.example.matchpredictor.service.MatchService;
import com.example.matchpredictor.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/predictions")
public class TeamPredictionController {

    private static final Logger log = LoggerFactory.getLogger(TeamPredictionController.class);

    @Autowired
    private AiPredictionService aiPredictionService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private TeamService teamService;

    /**
     * Generate RAG prediction between two teams (creates match if needed)
     */
    @PostMapping("/predict-teams")
    public ResponseEntity<?> predictBetweenTeams(@RequestBody TeamPredictionRequest request) {
        try {
            log.info("RAG prediction request: team {} vs team {}", request.getHomeTeamId(), request.getAwayTeamId());

            // Get teams from database
            Team homeTeam = teamService.getTeamById(request.getHomeTeamId())
                    .orElseThrow(() -> new RuntimeException("Home team not found"));

            Team awayTeam = teamService.getTeamById(request.getAwayTeamId())
                    .orElseThrow(() -> new RuntimeException("Away team not found"));

            if (homeTeam.getId().equals(awayTeam.getId())) {
                return ResponseEntity.badRequest().body("Teams must be different");
            }

            // Check if match already exists
            Optional<Match> existingMatch = findMatchBetweenTeams(homeTeam, awayTeam);

            Match match;
            if (existingMatch.isPresent()) {
                log.info("Found existing match: {}", existingMatch.get().getId());
                match = existingMatch.get();
            } else {
                // Create new match for RAG prediction
                log.info("Creating new match for RAG prediction");
                match = new Match(homeTeam, awayTeam,
                        LocalDateTime.now().plusDays(7),
                        "Prediction Request");
                match.setVenue(homeTeam.getName() + " Stadium");
                match = matchService.createMatch(match);
                log.info("Match created with id {}", match.getId());
            }

            // Generate AI prediction using RAG
            // This will use your existing AiPredictionService.generatePrediction()
            // which already has RAG with ChromaDB, PostgreSQL stats, etc.
            log.info("Generating RAG prediction with real data");
            AiPrediction prediction = aiPredictionService.generatePrediction(match.getId());

            log.info("RAG prediction generated successfully");
            return ResponseEntity.ok(prediction);

        } catch (Exception e) {
            log.error("Error generating RAG prediction", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private Optional<Match> findMatchBetweenTeams(Team team1, Team team2) {
        return matchService.getUpcomingMatches().stream()
                .filter(m ->
                        (m.getHomeTeam().getId().equals(team1.getId()) &&
                                m.getAwayTeam().getId().equals(team2.getId())) ||
                                (m.getHomeTeam().getId().equals(team2.getId()) &&
                                        m.getAwayTeam().getId().equals(team1.getId()))
                )
                .findFirst();
    }
}
