package com.example.matchpredictor.controller;

import com.example.matchpredictor.service.MatchService;
import com.example.matchpredictor.service.TeamService;
import com.example.matchpredictor.service.AiPredictionService;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.entity.AiPrediction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class SimpleChatController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private AiPredictionService aiPredictionService;

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "response", "Please ask me about a match prediction!"
            ));
        }

        String response = generateResponse(message.toLowerCase());

        return ResponseEntity.ok(Map.of(
                "response", response
        ));
    }

    private String generateResponse(String message) {
        // Greetings
        if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hello! I'm your AI football prediction assistant powered by RAG (Retrieval-Augmented Generation). " +
                    "I use real historical data and Ollama's LLaMA 3.2 to make predictions!\n\n" +
                    "Try: 'Chelsea vs Arsenal' or 'Real Madrid vs Barcelona'";
        }

        // Help
        if (message.contains("help")) {
            return "I can help you with AI-powered match predictions using RAG! Try asking:\n" +
                    "- 'Real Madrid vs Barcelona'\n" +
                    "- 'Who will win Liverpool vs Chelsea?'\n" +
                    "- 'Show me upcoming matches'\n" +
                    "- 'List all teams'";
        }

        // List teams
        if (message.contains("teams") || message.contains("list teams")) {
            List<Team> teams = teamService.getAllTeams();
            if (teams.isEmpty()) {
                return "No teams found in the database. Please add some teams first!";
            }

            String teamList = teams.stream()
                    .limit(10)
                    .map(t -> t.getName() + " (" + t.getCountry() + ")")
                    .collect(Collectors.joining(", "));

            return "Available teams: " + teamList +
                    (teams.size() > 10 ? " and " + (teams.size() - 10) + " more..." : "");
        }

        // List matches
        if (message.contains("matches") || message.contains("upcoming")) {
            List<Match> matches = matchService.getUpcomingMatches();
            if (matches.isEmpty()) {
                return "No upcoming matches scheduled. Please create some matches first!";
            }

            String matchList = matches.stream()
                    .limit(5)
                    .map(m -> m.getHomeTeam().getName() + " vs " + m.getAwayTeam().getName() +
                            " (" + m.getLeague() + ")")
                    .collect(Collectors.joining("\n"));

            return "Upcoming matches:\n" + matchList;
        }

        // Predict specific match using RAG
        if (message.contains("vs") || message.contains("predict")) {
            String[] parts = message.split("vs");
            if (parts.length == 2) {
                String team1Name = parts[0].trim()
                        .replace("predict", "")
                        .replace("who will win", "")
                        .replace("between", "")
                        .trim();
                String team2Name = parts[1].trim()
                        .replace("?", "")
                        .trim();

                return generateRAGPrediction(team1Name, team2Name); // ← use RAG
            }
        }

        // Check if message contains specific team names
        List<Team> allTeams = teamService.getAllTeams();
        for (Team team : allTeams) {
            if (message.contains(team.getName().toLowerCase())) {
                return String.format(
                        "I see you mentioned %s! They're from %s and were founded in %d. " +
                                "Would you like to know about their upcoming matches or make a prediction?",
                        team.getName(),
                        team.getCountry(),
                        team.getFoundedYear() != null ? team.getFoundedYear() : 0
                );
            }
        }

        // Default response
        return "I'm not sure I understand. Try asking about:\n" +
                "- Specific matches (e.g., 'Real Madrid vs Barcelona')\n" +
                "- 'Show upcoming matches'\n" +
                "- 'List all teams'\n" +
                "- Type 'help' for more options";
    }

    /**
     * Generate RAG-powered prediction using AiPredictionService
     */
    private String generateRAGPrediction(String team1, String team2) {
        try {
            // Find teams in database
            List<Team> allTeams = teamService.getAllTeams();

            Team foundTeam1 = allTeams.stream()
                    .filter(t -> t.getName().toLowerCase().contains(team1.toLowerCase()) ||
                            team1.toLowerCase().contains(t.getName().toLowerCase()))
                    .findFirst()
                    .orElse(null);

            Team foundTeam2 = allTeams.stream()
                    .filter(t -> t.getName().toLowerCase().contains(team2.toLowerCase()) ||
                            team2.toLowerCase().contains(t.getName().toLowerCase()))
                    .findFirst()
                    .orElse(null);

            if (foundTeam1 == null || foundTeam2 == null) {
                return String.format(
                        "I couldn't find both teams in the database.\n\n" +
                                "Available teams: %s",
                        allTeams.stream().limit(10).map(Team::getName).collect(Collectors.joining(", "))
                );
            }

            // Check if match already exists
            List<Match> upcomingMatches = matchService.getUpcomingMatches();
            Match existingMatch = upcomingMatches.stream()
                    .filter(m ->
                            (m.getHomeTeam().getId().equals(foundTeam1.getId()) &&
                                    m.getAwayTeam().getId().equals(foundTeam2.getId())) ||
                                    (m.getHomeTeam().getId().equals(foundTeam2.getId()) &&
                                            m.getAwayTeam().getId().equals(foundTeam1.getId()))
                    )
                    .findFirst()
                    .orElse(null);

            Match matchToPredict;

            // If match doesn't exist, create a temporary one
            if (existingMatch == null) {
                System.out.println("Creating temporary match for prediction...");
                Match tempMatch = new Match();
                tempMatch.setHomeTeam(foundTeam1);
                tempMatch.setAwayTeam(foundTeam2);
                tempMatch.setLeague("Friendly/Chat Prediction");
                tempMatch.setMatchDate(LocalDateTime.now().plusDays(7));
                tempMatch.setStatus("SCHEDULED");

                // Save temporary match
                matchToPredict = matchService.createMatch(tempMatch);
            } else {
                matchToPredict = existingMatch;
            }

            // Generate RAG prediction using AiPredictionService
            System.out.println("Generating RAG prediction for match ID: " + matchToPredict.getId());
            AiPrediction prediction = aiPredictionService.generatePrediction(matchToPredict.getId());

            // Format the response
            return String.format(
                    "**AI-Powered RAG Prediction**\n\n" +
                            "**%s vs %s**\n" +
                            "League: %s\n\n" +
                            "**Probabilities:**\n" +
                            "%s: %.1f%%\n" +
                            "Draw: %.1f%%\n" +
                            "%s: %.1f%%\n\n" +
                            "**AI Reasoning:**\n%s\n\n" +
                            "Confidence: %.0f%%\n" +
                            "Model: %s",
                    foundTeam1.getName(),
                    foundTeam2.getName(),
                    matchToPredict.getLeague(),
                    foundTeam1.getName(),
                    prediction.getHomeWinProbability(),
                    prediction.getDrawProbability(),
                    foundTeam2.getName(),
                    prediction.getAwayWinProbability(),
                    prediction.getReasoning(),
                    prediction.getConfidenceScore().doubleValue() * 100,
                    prediction.getModelVersion() != null ? prediction.getModelVersion() : "llama3.2-RAG"
            );

        } catch (Exception e) {
            e.printStackTrace();
            return "**Error generating AI prediction**\n\n" +
                    "Make sure:\n" +
                    "Ollama is running (http://localhost:11434)\n" +
                    "llama3.2 model is installed (`ollama pull llama3.2`)\n" +
                    "Database is connected\n\n" +
                    "Error details: " + e.getMessage();
        }
    }
}