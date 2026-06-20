package com.example.matchpredictor.controller;

import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.service.AiPredictionService;
import com.example.matchpredictor.service.MatchService;
import com.example.matchpredictor.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/predictions")
public class MatchPredictionController {

    @Autowired
    private AiPredictionService aiPredictionService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private TeamService teamService;

    // Generate prediction for a match
    @PostMapping("/generate/{matchId}")
    public ResponseEntity<?> generatePrediction(@PathVariable Integer matchId) {
        try {
            AiPrediction prediction = aiPredictionService.generatePrediction(matchId);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Get predictions for a match
    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<AiPrediction>> getPredictionsForMatch(@PathVariable Integer matchId) {
        List<AiPrediction> predictions = aiPredictionService.getPredictionsForMatch(matchId);
        return ResponseEntity.ok(predictions);
    }

    // Get latest prediction for a match
    @GetMapping("/match/{matchId}/latest")
    public ResponseEntity<?> getLatestPrediction(@PathVariable Integer matchId) {
        Optional<AiPrediction> prediction = aiPredictionService.getLatestPrediction(matchId);

        if (prediction.isPresent()) {
            return ResponseEntity.ok(prediction.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Get all predictions
    @GetMapping("/all")
    public ResponseEntity<List<AiPrediction>> getAllPredictions() {
        List<AiPrediction> predictions = aiPredictionService.getAllPredictions();
        return ResponseEntity.ok(predictions);
    }

    // Get prediction accuracy stats
    @GetMapping("/stats")
    public ResponseEntity<String> getPredictionStats() {
        String stats = aiPredictionService.getPredictionStats();
        return ResponseEntity.ok(stats);
    }

    // Get upcoming matches (for prediction)
    @GetMapping("/upcoming-matches")
    public ResponseEntity<List<Match>> getUpcomingMatches() {
        List<Match> matches = matchService.getUpcomingMatches();
        return ResponseEntity.ok(matches);
    }

    // Get all teams (for creating matches)
    @GetMapping("/teams")
    public ResponseEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamService.getAllTeams();
        return ResponseEntity.ok(teams);
    }
}
