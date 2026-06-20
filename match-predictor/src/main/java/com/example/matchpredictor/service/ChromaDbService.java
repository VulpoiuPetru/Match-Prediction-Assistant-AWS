package com.example.matchpredictor.service;

import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.repository.AiPredictionRepository;
import com.example.matchpredictor.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChromaDbService {

    @Autowired
    private AiPredictionRepository aiPredictionRepository;

    @Autowired
    private MatchRepository matchRepository;

    private Object client;
    private Object predictionCollection;
    private Object matchHistoryCollection;
    private Object analyticsCollection;
    private boolean isConnected = false;

    @PostConstruct
    public void init() {
        System.out.println("CHROMADB CONNECTION DEBUG:");

        try {
            System.out.println("1. Loading classes...");
            Class<?> clientClass = Class.forName("tech.amikos.chromadb.Client");
            Class<?> embeddingFunctionClass = Class.forName("tech.amikos.chromadb.embeddings.EmbeddingFunction");
            System.out.println("   ✓ Classes loaded");

            System.out.println("2. Creating client...");
            client = clientClass.getConstructor(String.class)
                    .newInstance("http://localhost:8000");
            System.out.println("   ✓ Client created");

            System.out.println("3. Looking for default embedding function...");
            Object defaultEmbeddingFunction = null;

            // Try to find a default embedding function
            try {
                Class<?> defaultEFClass = Class.forName("tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction");
                defaultEmbeddingFunction = defaultEFClass.getConstructor().newInstance();
                System.out.println("   ✓ Using DefaultEmbeddingFunction");
            } catch (ClassNotFoundException e1) {
                // Try OpenAI embedding function with null (some versions allow it)
                try {
                    Class<?> openAIEFClass = Class.forName("tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction");
                    // Leave as null - we'll handle this differently
                    System.out.println("   ℹ️ No default embedding function found, trying without embeddings");
                } catch (ClassNotFoundException e2) {
                    System.out.println("   ⚠️ No embedding function classes found");
                }
            }

            System.out.println("4. Getting collection methods...");
            var createCollectionMethod = clientClass.getMethod("createCollection",
                    String.class, Map.class, Boolean.class, embeddingFunctionClass);
            var getCollectionMethod = clientClass.getMethod("getCollection",
                    String.class, embeddingFunctionClass);
            System.out.println("   ✓ Methods found");

            System.out.println("5. Creating collections...");

            // Create collections
            predictionCollection = getOrCreateCollection(
                    "match_predictions", createCollectionMethod, getCollectionMethod, defaultEmbeddingFunction);
            System.out.println("   ✓ match_predictions ready");

            matchHistoryCollection = getOrCreateCollection(
                    "match_history", createCollectionMethod, getCollectionMethod, defaultEmbeddingFunction);
            System.out.println("   ✓ match_history ready");

            analyticsCollection = getOrCreateCollection(
                    "prediction_analytics", createCollectionMethod, getCollectionMethod, defaultEmbeddingFunction);
            System.out.println("   ✓ prediction_analytics ready");

            isConnected = true;
            System.out.println("✅ ChromaDB connected successfully!");
            System.out.println("   📊 Collections: predictions, match_history, analytics");

            initializeCollections();

        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getClass().getSimpleName());
            System.out.println("   Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   Cause: " + e.getCause().getMessage());
            }
            e.printStackTrace();
            isConnected = false;
        }

        System.out.println("=== END DEBUG ===");
    }

    private Object getOrCreateCollection(String name,
                                         java.lang.reflect.Method createMethod,
                                         java.lang.reflect.Method getMethod,
                                         Object embeddingFunction) throws Exception {
        try {
            System.out.println("   Trying to get existing collection: " + name);
            return getMethod.invoke(client, name, embeddingFunction);
        } catch (Exception e) {
            System.out.println("   Collection not found, creating: " + name);
            // If embedding function is still null, skip for now
            if (embeddingFunction == null) {
                System.out.println("   ⚠️ Skipping collection creation (no embedding function)");
                throw new RuntimeException("Cannot create collection without embedding function");
            }
            return createMethod.invoke(client, name, null, true, embeddingFunction);
        }
    }

    /**
     * MARK 9 REQUIREMENT: Initialize collections with existing data
     */
    private void initializeCollections() {
        if (!isConnected) return;

        try {
            // Store all existing predictions
            List<AiPrediction> predictions = aiPredictionRepository.findAll();
            System.out.println("📥 Loading " + predictions.size() + " predictions into ChromaDB...");

            for (AiPrediction pred : predictions) {
                storePrediction(pred);
            }

            // Store all finished matches for context reuse
            List<Match> matches = matchRepository.findAll()
                    .stream()
                    .filter(m -> "FINISHED".equals(m.getStatus()))
                    .toList();

            System.out.println("📥 Loading " + matches.size() + " finished matches...");
            for (Match match : matches) {
                storeMatchHistory(match);
            }

            // Generate analytics
            generateAnalytics();

            System.out.println("✅ ChromaDB initialization complete!");

        } catch (Exception e) {
            System.out.println("⚠️ Initialization failed: " + e.getMessage());
        }
    }

    /**
     * MARK 9 - HISTORY TRACKING: Store prediction with rich metadata
     */
    public void storePrediction(AiPrediction prediction) {
        if (!isConnected || predictionCollection == null) {
            System.out.println("ℹ️ ChromaDB not available - skipping vector storage");
            return;
        }

        try {
            String id = "prediction_" + prediction.getId();
            Match match = prediction.getMatch();

            // Rich semantic document for vector search
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

            var addMethod = predictionCollection.getClass().getMethod("add",
                    List.class, List.class, List.class, List.class);
            addMethod.invoke(predictionCollection,
                    null, List.of(metadata), List.of(document), List.of(id));

            System.out.println("✅ Stored prediction in ChromaDB: " + id);

        } catch (Exception e) {
            System.out.println("⚠️ ChromaDB storage failed: " + e.getMessage());
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

            var addMethod = matchHistoryCollection.getClass().getMethod("add",
                    List.class, List.class, List.class, List.class);
            addMethod.invoke(matchHistoryCollection,
                    null, List.of(metadata), List.of(document), List.of(id));

        } catch (Exception e) {
            System.out.println("⚠️ Failed to store match history: " + e.getMessage());
        }
    }

    /**
     * MARK 9 - ANALYTICS: Generate and store prediction analytics
     */
    public void generateAnalytics() {
        if (!isConnected || analyticsCollection == null) return;

        try {
            List<AiPrediction> allPredictions = aiPredictionRepository.findAll();

            // Calculate various analytics
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

            // Create analytics document
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

            var addMethod = analyticsCollection.getClass().getMethod("add",
                    List.class, List.class, List.class, List.class);
            addMethod.invoke(analyticsCollection,
                    null, List.of(metadata), List.of(analyticsDoc), List.of("analytics_latest"));

            System.out.println("✅ Analytics generated and stored in ChromaDB");

        } catch (Exception e) {
            System.out.println("⚠️ Analytics generation failed: " + e.getMessage());
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
            var queryMethod = predictionCollection.getClass().getMethod("query",
                    List.class, Integer.class, Map.class, Map.class, List.class);
            Object results = queryMethod.invoke(predictionCollection,
                    List.of(query), limit, null, null, null);

            var getDocsMethod = results.getClass().getMethod("getDocuments");
            @SuppressWarnings("unchecked")
            List<List<String>> docs = (List<List<String>>) getDocsMethod.invoke(results);

            if (docs != null && !docs.isEmpty()) {
                return docs.get(0);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Search failed: " + e.getMessage());
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

            var queryMethod = matchHistoryCollection.getClass().getMethod("query",
                    List.class, Integer.class, Map.class, Map.class, List.class);
            Object results = queryMethod.invoke(matchHistoryCollection,
                    List.of(query), 5, null, null, null);

            var getDocsMethod = results.getClass().getMethod("getDocuments");
            @SuppressWarnings("unchecked")
            List<List<String>> docs = (List<List<String>>) getDocsMethod.invoke(results);

            if (docs != null && !docs.isEmpty() && !docs.get(0).isEmpty()) {
                StringBuilder context = new StringBuilder();
                context.append("=== HISTORICAL CONTEXT FROM CHROMADB ===\n\n");
                for (String doc : docs.get(0)) {
                    context.append(doc).append("\n\n");
                }
                return context.toString();
            }
        } catch (Exception e) {
            System.out.println("⚠️ Context retrieval failed: " + e.getMessage());
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

            // Get latest analytics document
            if (analyticsCollection != null) {
                var queryMethod = analyticsCollection.getClass().getMethod("query",
                        List.class, Integer.class, Map.class, Map.class, List.class);
                Object results = queryMethod.invoke(analyticsCollection,
                        List.of("latest analytics summary"), 1, null, null, null);

                var getDocsMethod = results.getClass().getMethod("getDocuments");
                @SuppressWarnings("unchecked")
                List<List<String>> docs = (List<List<String>>) getDocsMethod.invoke(results);

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
            var countMethod = predictionCollection.getClass().getMethod("count");
            return (Integer) countMethod.invoke(predictionCollection);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getMatchHistoryCount() {
        if (!isConnected || matchHistoryCollection == null) return 0;
        try {
            var countMethod = matchHistoryCollection.getClass().getMethod("count");
            return (Integer) countMethod.invoke(matchHistoryCollection);
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
