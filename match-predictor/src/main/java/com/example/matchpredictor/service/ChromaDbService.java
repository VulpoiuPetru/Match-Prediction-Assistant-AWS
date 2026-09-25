package com.example.matchpredictor.service;

import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.repository.AiPredictionRepository;
import com.example.matchpredictor.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * NOTE: this class used to talk to the ChromaDB Java client entirely through reflection
 * (Class.forName + Method.invoke on Object fields), even though the real, typed
 * chromadb-java-client:0.1.7 dependency was already on the classpath (see pom.xml).
 * Rewritten to use the typed Client/Collection/EmbeddingFunction API directly.
 *
 * IMPORTANT: this file could not be compiled against the real dependency in the
 * environment this rewrite was done in (no network access to Maven Central), so the
 * class/method names below are reconstructed from the exact strings the previous
 * reflection code used (Class.forName("tech.amikos.chromadb.Client"), method
 * signatures via getMethod(...)), which is strong but not 100% verified evidence.
 * Run `mvnw compile` (or `mvnw.cmd compile` on Windows) locally before committing.
 * If it fails, the most likely fix is a package name tweak (e.g. EmbeddingFunction /
 * DefaultEmbeddingFunction living directly under tech.amikos.chromadb instead of
 * tech.amikos.chromadb.embeddings) rather than a structural rewrite.
 */
@Service
public class ChromaDbService {

    private static final Logger log = LoggerFactory.getLogger(ChromaDbService.class);

    @Autowired
    private AiPredictionRepository aiPredictionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Value("${chromadb.url:http://localhost:8000}")
    private String chromaDbUrl;

    private Client client;
    private Collection predictionCollection;
    private Collection matchHistoryCollection;
    private Collection analyticsCollection;
    private boolean isConnected = false;

    @PostConstruct
    public void init() {
        try {
            client = new Client(chromaDbUrl);
            EmbeddingFunction embeddingFunction = new DefaultEmbeddingFunction();

            predictionCollection = getOrCreateCollection("match_predictions", embeddingFunction);
            matchHistoryCollection = getOrCreateCollection("match_history", embeddingFunction);
            analyticsCollection = getOrCreateCollection("prediction_analytics", embeddingFunction);

            isConnected = true;
            log.info("ChromaDB connected - collections: predictions, match_history, analytics");

            initializeCollections();

        } catch (Exception e) {
            log.error("ChromaDB connection failed", e);
            isConnected = false;
        }
    }

    private Collection getOrCreateCollection(String name, EmbeddingFunction embeddingFunction) throws Exception {
        try {
            return client.getCollection(name, embeddingFunction);
        } catch (Exception e) {
            return client.createCollection(name, null, true, embeddingFunction);
        }
    }

    /**
     * MARK 9 REQUIREMENT: Initialize collections with existing data
     */
    private void initializeCollections() {
        if (!isConnected) return;

        try {
            List<AiPrediction> predictions = aiPredictionRepository.findAll();
            log.info("Loading {} predictions into ChromaDB", predictions.size());
            for (AiPrediction pred : predictions) {
                storePrediction(pred);
            }

            List<Match> matches = matchRepository.findAll()
                    .stream()
                    .filter(m -> "FINISHED".equals(m.getStatus()))
                    .toList();

            log.info("Loading {} finished matches", matches.size());
            for (Match match : matches) {
                storeMatchHistory(match);
            }

            generateAnalytics();

            log.info("ChromaDB initialization complete");

        } catch (Exception e) {
            log.error("ChromaDB initialization failed", e);
        }
    }

    /**
     * MARK 9 - HISTORY TRACKING: Store prediction with rich metadata
     */
    public void storePrediction(AiPrediction prediction) {
        if (!isConnected || predictionCollection == null) {
            log.warn("ChromaDB not available - skipping vector storage");
            return;
        }

        try {
            String id = "prediction_" + prediction.getId();
            Match match = prediction.getMatch();

            String document = String.format("""
                AI Prediction for %s vs %s in %s league.
                Predicted probabilities: Home win %.2f%%, Draw %.2f%%, Away win %.2f%%.
                AI reasoning: %s
                Match details: Venue %s, Date %s.
                Model version: %s, Confidence: %.2f.
                This prediction was made for a %s match.
                """,
                    match.getHomeTeam().getName(),
                    match.getAwayTeam().getName(),
                    match.getLeague(),
                    prediction.getHomeWinProbability(),
                    prediction.getDrawProbability(),
                    prediction.getAwayWinProbability(),
                    prediction.getReasoning(),
                    match.getVenue() != null ? match.getVenue() : "TBD",
                    match.getMatchDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    prediction.getModelVersion() != null ? prediction.getModelVersion() : "unknown",
                    prediction.getConfidenceScore() != null ? prediction.getConfidenceScore() : 0.0,
                    match.getLeague()
            );

            Map<String, String> metadata = new HashMap<>();
            metadata.put("prediction_id", prediction.getId().toString());
            metadata.put("match_id", match.getId().toString());
            metadata.put("home_team", match.getHomeTeam().getName());
            metadata.put("away_team", match.getAwayTeam().getName());
            metadata.put("league", match.getLeague());
            metadata.put("home_prob", prediction.getHomeWinProbability().toString());
            metadata.put("draw_prob", prediction.getDrawProbability().toString());
            metadata.put("away_prob", prediction.getAwayWinProbability().toString());
            metadata.put("timestamp", prediction.getCreatedAt().toString());

            predictionCollection.add(null, List.of(metadata), List.of(document), List.of(id));

            log.info("Stored prediction in ChromaDB: {}", id);

        } catch (Exception e) {
            log.error("ChromaDB storage failed", e);
        }
    }

    /**
     * MARK 9 - CONTEXT REUSE: Store match history for future predictions
     */
    public void storeMatchHistory(Match match) {
        if (!isConnected || matchHistoryCollection == null) return;

        try {
            String id = "match_" + match.getId();

            String document = String.format("""
                Historical match: %s vs %s in %s.
                Final result: %s %d - %d %s.
                Venue: %s, Date: %s.
                Match outcome: %s.
                Goal difference: %d.
                %s performance: scored %d goals.
                %s performance: scored %d goals.
                This was a %s league match.
                """,
                    match.getHomeTeam().getName(),
                    match.getAwayTeam().getName(),
                    match.getLeague(),
                    match.getHomeTeam().getName(),
                    match.getHomeScore(),
                    match.getAwayScore(),
                    match.getAwayTeam().getName(),
                    match.getVenue() != null ? match.getVenue() : "Unknown",
                    match.getMatchDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    determineOutcome(match),
                    Math.abs(match.getHomeScore() - match.getAwayScore()),
                    match.getHomeTeam().getName(),
                    match.getHomeScore(),
                    match.getAwayTeam().getName(),
                    match.getAwayScore(),
                    match.getLeague()
            );

            Map<String, String> metadata = new HashMap<>();
            metadata.put("match_id", match.getId().toString());
            metadata.put("home_team", match.getHomeTeam().getName());
            metadata.put("away_team", match.getAwayTeam().getName());
            metadata.put("result", String.format("%d-%d", match.getHomeScore(), match.getAwayScore()));
            metadata.put("winner", getWinner(match));
            metadata.put("league", match.getLeague());

            matchHistoryCollection.add(null, List.of(metadata), List.of(document), List.of(id));

        } catch (Exception e) {
            log.error("Failed to store match history", e);
        }
    }

    /**
     * MARK 9 - ANALYTICS: Generate and store prediction analytics
     */
    public void generateAnalytics() {
        if (!isConnected || analyticsCollection == null) return;

        try {
            List<AiPrediction> allPredictions = aiPredictionRepository.findAll();

            Map<String, Integer> teamPredictionCount = new HashMap<>();
            Map<String, Integer> leaguePredictionCount = new HashMap<>();
            double totalConfidence = 0;
            int correctPredictions = 0;
            int totalEvaluated = 0;

            for (AiPrediction pred : allPredictions) {
                String homeTeam = pred.getMatch().getHomeTeam().getName();
                String awayTeam = pred.getMatch().getAwayTeam().getName();
                String league = pred.getMatch().getLeague();

                teamPredictionCount.merge(homeTeam, 1, Integer::sum);
                teamPredictionCount.merge(awayTeam, 1, Integer::sum);
                leaguePredictionCount.merge(league, 1, Integer::sum);

                if (pred.getConfidenceScore() != null) {
                    totalConfidence += pred.getConfidenceScore().doubleValue();
                }

                if (pred.getIsCorrect() != null) {
                    totalEvaluated++;
                    if (pred.getIsCorrect()) correctPredictions++;
                }
            }

            String analyticsDoc = String.format("""
                Prediction System Analytics Summary:
                Total predictions made: %d
                Average confidence score: %.2f
                Evaluated predictions: %d
                Correct predictions: %d
                Accuracy rate: %.1f%%
                Most predicted teams: %s
                Most predicted leagues: %s
                System performance: %s
                """,
                    allPredictions.size(),
                    allPredictions.isEmpty() ? 0 : totalConfidence / allPredictions.size(),
                    totalEvaluated,
                    correctPredictions,
                    totalEvaluated > 0 ? (correctPredictions * 100.0 / totalEvaluated) : 0,
                    getTopEntries(teamPredictionCount, 3),
                    getTopEntries(leaguePredictionCount, 3),
                    totalEvaluated > 0 ?
                            (correctPredictions * 100.0 / totalEvaluated > 60 ? "Good" : "Needs improvement") :
                            "Not enough data"
            );

            Map<String, String> metadata = new HashMap<>();
            metadata.put("total_predictions", String.valueOf(allPredictions.size()));
            metadata.put("accuracy", String.valueOf(totalEvaluated > 0 ? (correctPredictions * 100.0 / totalEvaluated) : 0));
            metadata.put("timestamp", new java.util.Date().toString());

            analyticsCollection.add(null, List.of(metadata), List.of(analyticsDoc), List.of("analytics_latest"));

            log.info("Analytics generated and stored in ChromaDB");

        } catch (Exception e) {
            log.error("Analytics generation failed", e);
        }
    }

    /**
     * MARK 9 - CONTEXT REUSE: Search for similar predictions
     */
    public List<String> searchSimilarPredictions(String query, int limit) {
        if (!isConnected || predictionCollection == null) {
            return Collections.singletonList("ChromaDB not connected");
        }

        try {
            var results = predictionCollection.query(List.of(query), limit, null, null, null);
            List<List<String>> docs = results.getDocuments();

            if (docs != null && !docs.isEmpty()) {
                return docs.get(0);
            }
        } catch (Exception e) {
            log.error("Search failed", e);
        }

        return Collections.emptyList();
    }

    /**
     * MARK 9 - CONTEXT REUSE: Get relevant historical context
     */
    public String getHistoricalContext(String homeTeam, String awayTeam) {
        if (!isConnected || matchHistoryCollection == null) {
            return "No historical context available.";
        }

        try {
            String query = String.format(
                    "matches between %s and %s, their results and performance patterns",
                    homeTeam, awayTeam
            );

            var results = matchHistoryCollection.query(List.of(query), 5, null, null, null);
            List<List<String>> docs = results.getDocuments();

            if (docs != null && !docs.isEmpty() && !docs.get(0).isEmpty()) {
                StringBuilder context = new StringBuilder();
                context.append("=== HISTORICAL CONTEXT FROM CHROMADB ===\n\n");
                for (String doc : docs.get(0)) {
                    context.append(doc).append("\n\n");
                }
                return context.toString();
            }
        } catch (Exception e) {
            log.error("Context retrieval failed", e);
        }

        return "No relevant historical context found.";
    }

    /**
     * MARK 9 - ANALYTICS: Get prediction statistics
     */
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        if (!isConnected) {
            analytics.put("connected", false);
            analytics.put("message", "ChromaDB not connected");
            return analytics;
        }

        try {
            analytics.put("connected", true);
            analytics.put("predictionCount", getPredictionCount());
            analytics.put("matchHistoryCount", getMatchHistoryCount());

            if (analyticsCollection != null) {
                var results = analyticsCollection.query(List.of("latest analytics summary"), 1, null, null, null);
                List<List<String>> docs = results.getDocuments();

                if (docs != null && !docs.isEmpty() && !docs.get(0).isEmpty()) {
                    analytics.put("summary", docs.get(0).get(0));
                }
            }
        } catch (Exception e) {
            analytics.put("error", e.getMessage());
        }

        return analytics;
    }

    // Helper methods
    private String getWinner(Match match) {
        if (match.getHomeScore() > match.getAwayScore()) {
            return match.getHomeTeam().getName();
        } else if (match.getAwayScore() > match.getHomeScore()) {
            return match.getAwayTeam().getName();
        }
        return "Draw";
    }

    private String determineOutcome(Match match) {
        int diff = match.getHomeScore() - match.getAwayScore();
        if (diff > 0) return "Home win by " + diff + " goal(s)";
        if (diff < 0) return "Away win by " + Math.abs(diff) + " goal(s)";
        return "Draw";
    }

    private String getTopEntries(Map<String, Integer> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
    }

    public boolean isConnected() { return isConnected; }

    public int getPredictionCount() {
        if (!isConnected || predictionCollection == null) return 0;
        try {
            return predictionCollection.count();
        } catch (Exception e) {
            return 0;
        }
    }

    public int getMatchHistoryCount() {
        if (!isConnected || matchHistoryCollection == null) return 0;
        try {
            return matchHistoryCollection.count();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<String> getPredictionsByTeam(String teamName, int limit) {
        return searchSimilarPredictions(teamName + " predictions analysis", limit);
    }

    public String getStatusMessage() {
        if (isConnected) {
            return String.format("ChromaDB connected - %d predictions, %d matches in history",
                    getPredictionCount(), getMatchHistoryCount());
        }
        return "ChromaDB not connected";
    }

}
