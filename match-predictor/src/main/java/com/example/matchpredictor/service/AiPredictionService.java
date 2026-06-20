package com.example.matchpredictor.service;

import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.repository.AiPredictionRepository;
import com.example.matchpredictor.repository.MatchRepository;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AiPredictionService {

    @Autowired
    private AiPredictionRepository aiPredictionRepository;

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private OllamaChatClient ollamaChatClient;

    @Autowired
    private ChromaDbService chromaDbService;


    /**
     * Generate prediction using RAG approach:
     * 1. RETRIEVE relevant historical data from ChromaDB
     * 2. AUGMENT the prompt with this data
     * 3. GENERATE AI response based on enriched context
     */
    public AiPrediction generatePrediction(Integer matchId) {
        Match match = matchService.getMatchById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        // ============ RAG STEP 1: RETRIEVE ============
        System.out.println("RAG Step 1: Retrieving relevant data from ChromaDB...");
        String retrievedContext = retrieveRelevantContext(match);

        // ============ RAG STEP 2: AUGMENT ============
        System.out.println("RAG Step 2: Augmenting prompt with retrieved data...");
        String augmentedPrompt = createRAGPrompt(match, retrievedContext);

        try {
            // ============ RAG STEP 3: GENERATE ============
            System.out.println("RAG Step 3: Generating AI response...");
            OllamaApi directApi = new OllamaApi("http://localhost:11434");
            OllamaChatClient directClient = new OllamaChatClient(directApi);

            ChatResponse response = directClient.call(
                    new Prompt(augmentedPrompt,
                            org.springframework.ai.ollama.api.OllamaOptions.create()
                                    .withModel("llama3.2"))
            );

            String aiResponse = response.getResult().getOutput().getContent();
            AiPrediction prediction = parseAiResponse(match, aiResponse);
            prediction.setModelVersion("llama3.2-RAG"); // Mark it as RAG-enhanced

            // Store RAG context used in parameters
            prediction.setParameters(String.format(
                    "{\"rag_enabled\":true,\"context_sources\":\"chromadb\",\"retrieved_matches\":%d}",
                    countRetrievedMatches(retrievedContext)
            ));

            // Save to PostgreSQL
            AiPrediction savedPrediction = aiPredictionRepository.save(prediction);

            // Store back in ChromaDB for future RAG retrievals
            chromaDbService.storePrediction(savedPrediction);

            System.out.println("RAG prediction completed and stored!");
            return savedPrediction;

        } catch (Exception e) {
            throw new RuntimeException("Error generating RAG prediction: " + e.getMessage());
        }
    }


     // RAG STEP 1: Retrieve relevant historical context from multiple sources

    private String retrieveRelevantContext(Match match) {
        StringBuilder context = new StringBuilder();

        // 1. Get historical context from ChromaDB (vector search)
        if (chromaDbService.isConnected()) {
            System.out.println("Searching ChromaDB vector store...");
            String chromaContext = chromaDbService.getHistoricalContext(
                    match.getHomeTeam().getName(),
                    match.getAwayTeam().getName()
            );

            if (chromaContext != null && !chromaContext.contains("No relevant")) {
                context.append(chromaContext);
            }
        }

        // 2. Get head-to-head statistics from PostgreSQL
        System.out.println("Querying PostgreSQL for head-to-head...");
        String h2hStats = getHeadToHeadStats(match);
        context.append(h2hStats);

        // 3. Get recent form from PostgreSQL
        System.out.println("Analyzing recent form...");
        String formStats = getTeamFormStats(match);
        context.append(formStats);

        // 4. Get similar predictions from ChromaDB
        if (chromaDbService.isConnected()) {
            System.out.println("Finding similar past predictions...");
            List<String> similarPredictions = chromaDbService.searchSimilarPredictions(
                    String.format("predictions for %s vs %s or similar matchups in %s",
                            match.getHomeTeam().getName(),
                            match.getAwayTeam().getName(),
                            match.getLeague()),
                    3
            );

            if (!similarPredictions.isEmpty() && !similarPredictions.get(0).contains("not connected")) {
                context.append("\n=== SIMILAR PAST PREDICTIONS ===\n");
                for (int i = 0; i < similarPredictions.size(); i++) {
                    context.append(String.format("%d. %s\n", i + 1, similarPredictions.get(i)));
                }
            }
        }

        return context.toString();
    }


     //RAG STEP 2: Create augmented prompt with retrieved context

    private String createRAGPrompt(Match match, String retrievedContext) {
        return String.format("""
            You are a professional football analyst using RAG (Retrieval-Augmented Generation).
            You have access to REAL HISTORICAL DATA retrieved from our database.
            
            MATCH TO PREDICT:
            Home Team: %s (%s)
            Away Team: %s (%s)
            League: %s
            Venue: %s
            Date: %s
            
            RETRIEVED CONTEXT (from our database)
            %s
            
            YOUR TASK:
            Based on the RETRIEVED HISTORICAL DATA above (not general knowledge), predict:
            1. Home Win Probability (0-100)
            2. Draw Probability (0-100)
            3. Away Win Probability (0-100)
            4. Reasoning that SPECIFICALLY REFERENCES the retrieved data
            
            CRITICAL REQUIREMENTS:
            - Your prediction MUST cite specific facts from the retrieved context
            - Example: "Based on the 3-1 head-to-head advantage shown in the data..."
            - Do NOT use general football knowledge - ONLY use the retrieved context
            - If no relevant data was retrieved, state this clearly
            - Probabilities must sum to exactly 100
            
            Format your response as:
            HOME_WIN: [percentage]
            DRAW: [percentage]
            AWAY_WIN: [percentage]
            REASONING: [analysis citing specific retrieved data points]
            """,
                match.getHomeTeam().getName(),
                match.getHomeTeam().getCountry(),
                match.getAwayTeam().getName(),
                match.getAwayTeam().getCountry(),
                match.getLeague(),
                match.getVenue() != null ? match.getVenue() : "TBD",
                match.getMatchDate(),
                retrievedContext.isEmpty() ?
                        "No historical data available for these teams. Base prediction on general patterns." :
                        retrievedContext
        );
    }


     // Get head-to-head statistics from database

    private String getHeadToHeadStats(Match match) {
        List<Match> h2hMatches = matchRepository.findMatchesBetweenTeams(
                match.getHomeTeam(),
                match.getAwayTeam()
        );

        if (h2hMatches.isEmpty()) {
            return "\nHEAD-TO-HEAD\nNo previous matches found.\n\n";
        }

        StringBuilder stats = new StringBuilder("\nHEAD-TO-HEAD STATISTICS\n");
        stats.append(String.format("Total matches: %d\n", h2hMatches.size()));

        int homeWins = 0, draws = 0, awayWins = 0;
        int totalHomeGoals = 0, totalAwayGoals = 0;

        for (Match m : h2hMatches) {
            if ("FINISHED".equals(m.getStatus())) {
                boolean homeIsTeam1 = m.getHomeTeam().getId().equals(match.getHomeTeam().getId());
                int team1Score = homeIsTeam1 ? m.getHomeScore() : m.getAwayScore();
                int team2Score = homeIsTeam1 ? m.getAwayScore() : m.getHomeScore();

                totalHomeGoals += team1Score;
                totalAwayGoals += team2Score;

                if (team1Score > team2Score) homeWins++;
                else if (team1Score < team2Score) awayWins++;
                else draws++;
            }
        }

        int total = homeWins + draws + awayWins;
        if (total > 0) {
            stats.append(String.format("%s: %d wins (%.0f%%)\n",
                    match.getHomeTeam().getName(), homeWins, (homeWins * 100.0 / total)));
            stats.append(String.format("Draws: %d (%.0f%%)\n",
                    draws, (draws * 100.0 / total)));
            stats.append(String.format("%s: %d wins (%.0f%%)\n",
                    match.getAwayTeam().getName(), awayWins, (awayWins * 100.0 / total)));
            stats.append(String.format("Average goals: %s %.1f - %.1f %s\n\n",
                    match.getHomeTeam().getName(), (totalHomeGoals * 1.0 / total),
                    (totalAwayGoals * 1.0 / total), match.getAwayTeam().getName()));
        }

        return stats.toString();
    }


     // Get team form statistics

    private String getTeamFormStats(Match match) {
        StringBuilder stats = new StringBuilder();

        // Home team form
        stats.append(String.format("RECENT FORM \n", match.getHomeTeam().getName().toUpperCase()));
        stats.append(getTeamForm(match.getHomeTeam()));

        // Away team form
        stats.append(String.format("RECENT FORM\n", match.getAwayTeam().getName().toUpperCase()));
        stats.append(getTeamForm(match.getAwayTeam()));

        return stats.toString();
    }

    private String getTeamForm(com.example.matchpredictor.entity.Team team) {
        List<Match> recentMatches = matchRepository.findByTeam(team)
                .stream()
                .filter(m -> "FINISHED".equals(m.getStatus()))
                .sorted((m1, m2) -> m2.getMatchDate().compareTo(m1.getMatchDate()))
                .limit(5)
                .toList();

        if (recentMatches.isEmpty()) {
            return "No recent matches available.\n\n";
        }

        StringBuilder form = new StringBuilder();
        int wins = 0, draws = 0, losses = 0;

        form.append("Last 5 matches:\n");
        for (Match m : recentMatches) {
            boolean isHome = m.getHomeTeam().getId().equals(team.getId());
            int teamScore = isHome ? m.getHomeScore() : m.getAwayScore();
            int opponentScore = isHome ? m.getAwayScore() : m.getHomeScore();
            String opponent = isHome ? m.getAwayTeam().getName() : m.getHomeTeam().getName();

            char result;
            if (teamScore > opponentScore) {
                wins++;
                result = 'W';
            } else if (teamScore < opponentScore) {
                losses++;
                result = 'L';
            } else {
                draws++;
                result = 'D';
            }

            form.append(String.format("  vs %s: %d-%d (%c)\n", opponent, teamScore, opponentScore, result));
        }

        form.append(String.format("Form: %dW-%dD-%dL (%d points from 15)\n\n",
                wins, draws, losses, wins * 3 + draws));

        return form.toString();
    }

    private int countRetrievedMatches(String context) {
        // Simple count of how many matches were found in context
        return context.split("Match|match").length - 1;
    }

    // Keep all existing methods unchanged
    public List<AiPrediction> getPredictionsForMatch(Integer matchId) {
        Match match = matchService.getMatchById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));
        return aiPredictionRepository.findByMatch(match);
    }

    public Optional<AiPrediction> getLatestPrediction(Integer matchId) {
        Match match = matchService.getMatchById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + matchId));

        List<AiPrediction> predictions = aiPredictionRepository
                .findByMatchOrderByCreatedAtDesc(match, PageRequest.of(0, 1));

        return predictions.isEmpty() ? Optional.empty() : Optional.of(predictions.get(0));
    }

    public List<AiPrediction> getAllPredictions(){
        return aiPredictionRepository.findAll();
    }

    private AiPrediction parseAiResponse(Match match, String aiResponse) {
        AiPrediction prediction = new AiPrediction();
        prediction.setMatch(match);

        BigDecimal homeWin = new BigDecimal("33.33");
        BigDecimal draw = new BigDecimal("33.34");
        BigDecimal awayWin = new BigDecimal("33.33");

        StringBuilder reasoningBuilder = new StringBuilder();
        boolean inReasoning = false;

        String[] lines = aiResponse.split("\n");

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("HOME_WIN:")) {
                homeWin = new BigDecimal(line.replace("HOME_WIN:", "").replace("%", "").trim());
            } else if (line.startsWith("DRAW:")) {
                draw = new BigDecimal(line.replace("DRAW:", "").replace("%", "").trim());
            } else if (line.startsWith("AWAY_WIN:")) {
                awayWin = new BigDecimal(line.replace("AWAY_WIN:", "").replace("%", "").trim());
            } else if (line.startsWith("REASONING:")) {
                inReasoning = true;
                reasoningBuilder.append(line.replace("REASONING:", "").trim());
            } else if (inReasoning) {
                reasoningBuilder.append(" ").append(line);
            }
        }

        String reasoning = reasoningBuilder.toString().trim();

        // 🔐 FALLBACK OBLIGATORIU
        if (reasoning.isBlank()) {
            reasoning = "AI did not provide explicit reasoning. Full response: " + aiResponse;
        }

        prediction.setHomeWinProbability(homeWin);
        prediction.setDrawProbability(draw);
        prediction.setAwayWinProbability(awayWin);
        prediction.setReasoning(reasoning);
        prediction.setConfidenceScore(new BigDecimal("0.75"));

        return prediction;
    }


    public String getPredictionStats() {
        Long correct = aiPredictionRepository.countCorrectPredictions();
        Long total = aiPredictionRepository.countEvaluatedPredictions();

        if (total == 0) {
            return "No evaluated predictions yet";
        }

        double accuracy = (correct.doubleValue() / total.doubleValue()) * 100;
        return String.format("Accuracy: %.2f%% (%d correct out of %d predictions)", accuracy, correct, total);
    }
}
