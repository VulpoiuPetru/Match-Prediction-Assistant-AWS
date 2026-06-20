package com.example.matchpredictor.controller;

import com.example.matchpredictor.dto.ContextualPredictionRequest;
import com.example. matchpredictor. entity.AiPrediction;
import com.example.matchpredictor.entity.ConversationContext;
import com.example.matchpredictor.service.ContextualAiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory. annotation.Autowired;
import org. springframework.http.ResponseEntity;
import org.springframework.web. bind.annotation.*;

import java.util. List;
import java.util. Map;

@RestController
@RequestMapping("/api/contextual")
@CrossOrigin(origins = "*")
public class ContextualPredictionController {

    @Autowired
    private ContextualAiService contextualAiService;

    //Generate prediction with context (memory + parameters)
    @PostMapping("/predict")
    public ResponseEntity<? > generateContextualPrediction(
            @Valid @RequestBody ContextualPredictionRequest request) {
        try {
            AiPrediction prediction = contextualAiService.generateContextualPrediction(request);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "tip", "Make sure Ollama is running with llama3.2 model"
            ));
        }
    }

    //Get conversation history for a session (shows AI memory)
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ConversationContext>> getConversationHistory(
            @PathVariable String sessionId) {
        List<ConversationContext> history = contextualAiService. getConversationHistory(sessionId);
        return ResponseEntity. ok(history);
    }

    //Get available tones and roles
    @GetMapping("/options")
    public ResponseEntity<Map<String, List<String>>> getOptions() {
        return ResponseEntity.ok(Map.of(
                "tones", List.of("professional", "casual", "detailed", "brief"),
                "roles", List.of("analyst", "coach", "fan", "statistician")
        ));
    }

}
