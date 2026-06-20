package com.example.matchpredictor.controller;

import com.example.matchpredictor.service.ChromaDbService;
import org.springframework.beans. factory.annotation. Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework. web.bind.annotation.*;

import java. util.List;
import java.util. Map;

@RestController
@RequestMapping("/api/chromadb")
@CrossOrigin(origins = "*")
public class ChromaDbController {

    @Autowired
    private ChromaDbService chromaDbService;


     //Check ChromaDB connection status
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "connected", chromaDbService.isConnected(),
                "predictionCount", chromaDbService.getPredictionCount(),
                "message", chromaDbService.isConnected()
                        ? "ChromaDB is connected and ready"
                        : "ChromaDB is not connected.  Run:  docker run -p 8000:8000 chromadb/chroma"
        ));
    }


     // Search similar predictions using semantic search
    @GetMapping("/search")
    public ResponseEntity<? > searchPredictions(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {

        if (!chromaDbService.isConnected()) {
            return ResponseEntity.ok(Map.of(
                    "error", "ChromaDB not connected",
                    "results", List.of()
            ));
        }

        List<String> results = chromaDbService. searchSimilarPredictions(query, limit);
        return ResponseEntity.ok(Map.of(
                "query", query,
                "results", results,
                "count", results.size()
        ));
    }


     // Get prediction history for a specific team
    @GetMapping("/team/{teamName}")
    public ResponseEntity<?> getPredictionsByTeam(
            @PathVariable String teamName,
            @RequestParam(defaultValue = "10") int limit) {

        if (!chromaDbService.isConnected()) {
            return ResponseEntity.ok(Map.of(
                    "error", "ChromaDB not connected",
                    "results", List.of()
            ));
        }

        List<String> results = chromaDbService.getPredictionsByTeam(teamName, limit);
        return ResponseEntity.ok(Map.of(
                "team", teamName,
                "results", results,
                "count", results. size()
        ));
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        return ResponseEntity.ok(chromaDbService.getAnalytics());
    }

    @GetMapping("/history/{homeTeam}/{awayTeam}")
    public ResponseEntity<String> getHistoricalContext(
            @PathVariable String homeTeam,
            @PathVariable String awayTeam) {

        if (!chromaDbService.isConnected()) {
            return ResponseEntity.ok("ChromaDB not connected");
        }

        String context = chromaDbService.getHistoricalContext(homeTeam, awayTeam);
        return ResponseEntity.ok(context);
    }

    @PostMapping("/regenerate-analytics")
    public ResponseEntity<String> regenerateAnalytics() {
        if (!chromaDbService.isConnected()) {
            return ResponseEntity.ok("ChromaDB not connected");
        }

        chromaDbService.generateAnalytics();
        return ResponseEntity.ok("Analytics regenerated successfully");
    }
}
