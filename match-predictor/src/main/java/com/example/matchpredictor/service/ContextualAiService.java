package com.example.matchpredictor.service;

import com. example.matchpredictor.dto.ContextualPredictionRequest;
import com.example. matchpredictor. entity.AiPrediction;
import com.example.matchpredictor.entity.ConversationContext;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.repository.AiPredictionRepository;
import com.example.matchpredictor.repository. ConversationContextRepository;
import org.springframework.ai.chat.ChatResponse;
import org. springframework.ai.chat.prompt. Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org. springframework.ai.ollama.api.OllamaApi;
import org. springframework.ai.ollama.api.OllamaOptions;
import org. springframework.beans.factory.annotation. Autowired;
import org.springframework. data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java. util.List;
import java.util. UUID;

@Service
public class ContextualAiService {

    @Autowired
    private ConversationContextRepository contextRepository;

    @Autowired
    private AiPredictionRepository predictionRepository;

    @Autowired
    private MatchService matchService;

    //Generate a prediction with context awareness(mem+param)
    public AiPrediction generateContextualPrediction(ContextualPredictionRequest request) {
        Match match = matchService. getMatchById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + request.getMatchId()));

        // Generate session ID if not provided
        String sessionId = request.getSessionId() != null ?
                request.getSessionId() : UUID.randomUUID().toString();

        // Build context-aware prompt
        String prompt = buildContextualPrompt(match, request, sessionId);

        // Save the user's request to context
        ConversationContext userContext = new ConversationContext(
                sessionId,
                "Predict match:  " + match.getHomeTeam().getName() + " vs " + match.getAwayTeam().getName(),
                request.getTone(),
                request.getRole()
        );
        userContext.setMatch(match);
        try {
            // Create client with explicit model
            OllamaApi directApi = new OllamaApi("http://localhost:11434");
            OllamaChatClient directClient = new OllamaChatClient(directApi);

            ChatResponse response = directClient.call(
                    new Prompt(prompt, OllamaOptions.create().withModel("llama3.2"))
            );

            String aiResponse = response. getResult().getOutput().getContent();

            // Save AI response to context (for memory)
            userContext.setAiResponse(aiResponse);
            contextRepository.save(userContext);

            // Parse and save prediction
            AiPrediction prediction = parseAiResponse(match, aiResponse);
            prediction.setModelVersion("llama3.2");
            prediction.setParameters(buildParametersJson(request));

            return predictionRepository.save(prediction);

        } catch (Exception e) {
            throw new RuntimeException("Error generating contextual prediction:  " + e.getMessage(), e);
        }
    }

    //Build a prompt that includes conversation history and user preferences
    private String buildContextualPrompt(Match match, ContextualPredictionRequest request, String sessionId) {
        StringBuilder promptBuilder = new StringBuilder();

        // 1. Add role-based persona
        String persona = getRolePersona(request.getRole());
        promptBuilder.append(persona).append("\n\n");

        // 2. Add conversation history (MEMORY - this is what makes it context-aware!)
        List<ConversationContext> history = contextRepository.findRecentContext(sessionId, PageRequest.of(0, 5));
        if (!history.isEmpty()) {
            promptBuilder.append("PREVIOUS CONVERSATION CONTEXT:\n");
            // Reverse to show oldest first
            Collections.reverse(history);
            for (ConversationContext ctx : history) {
                promptBuilder.append("User asked: ").append(ctx.getUserMessage()).append("\n");
                if (ctx.getAiResponse() != null) {
                    // Summarize previous response to keep prompt shorter
                    String summary = ctx.getAiResponse().length() > 200
                            ? ctx.getAiResponse().substring(0, 200) + "..."
                            : ctx.getAiResponse();
                    promptBuilder.append("You responded: ").append(summary).append("\n\n");
                }
            }
            promptBuilder.append(" NOW CONTINUING THE CONVERSATION \n\n");
        }
        // 3. Add tone instruction
        String toneInstruction = getToneInstruction(request.getTone());
        promptBuilder.append(toneInstruction).append("\n\n");

        // 4. Add match details
        promptBuilder.append(String.format("""
                        Match Details:
                        - Home Team: %s (%s)
                        - Away Team: %s (%s)
                        - League: %s
                        - Venue: %s
                        - Date: %s
                        """,
                match.getHomeTeam().getName(),
                match.getHomeTeam().getCountry(),
                match.getAwayTeam().getName(),
                match.getAwayTeam().getCountry(),
                match.getLeague(),
                match.getVenue() != null ? match.getVenue() : "TBD",
                match.getMatchDate()
        ));

        // 5. Add additional context if provided
        if (request.getAdditionalContext() != null && !request.getAdditionalContext().isEmpty()) {
            promptBuilder.append("\nUser's specific focus: ").append(request.getAdditionalContext()).append("\n");
        }

        // 6. Add output format
        promptBuilder.append("""
                
                Please provide: 
                1. Home Win Probability (0-100)
                2. Draw Probability (0-100)
                3. Away Win Probability (0-100)
                4. Reasoning based on your role and the requested tone
                
                Format your response as: 
                HOME_WIN:  [percentage]
                DRAW: [percentage]
                AWAY_WIN: [percentage]
                REASONING: [your analysis]
                
                Make sure the probabilities add up to 100.
                """);

        return promptBuilder.toString();
    }

    private String getRolePersona(String role) {
        if (role == null) role = "analyst";

        return switch (role.toLowerCase()) {
            case "coach" -> "You are an experienced football coach analyzing this match from a tactical perspective.  Focus on formations, player matchups, and strategic advantages.";
            case "fan" -> "You are an enthusiastic football fan providing an engaging and passionate prediction. Share your excitement and gut feelings about the match.";
            case "statistician" -> "You are a data-driven sports statistician.  Base your analysis primarily on historical data, head-to-head records, and statistical trends.";
            default -> "You are a professional football analyst providing expert match predictions based on comprehensive analysis. ";
        };
    }

    private String getToneInstruction(String tone) {
        if (tone == null) tone = "professional";

        return switch (tone.toLowerCase()) {
            case "casual" -> "Respond in a casual, friendly manner.  Use conversational language.";
            case "detailed" -> "Provide an extremely detailed and thorough analysis. Leave no stone unturned.";
            case "brief" -> "Keep your response concise and to the point. Focus only on key factors.";
            default -> "Maintain a professional and balanced tone in your analysis.";
        };
    }

    private String buildParametersJson(ContextualPredictionRequest request) {
        return String.format("{\"tone\":\"%s\",\"role\":\"%s\",\"additionalContext\":\"%s\",\"sessionId\":\"%s\"}",
                request. getTone() != null ? request. getTone() : "professional",
                request. getRole() != null ? request.getRole() : "analyst",
                request.getAdditionalContext() != null ? request. getAdditionalContext().replace("\"", "'") : "",
                request.getSessionId() != null ? request.getSessionId() : "");
    }

    private AiPrediction parseAiResponse(Match match, String aiResponse) {
        AiPrediction prediction = new AiPrediction();
        prediction.setMatch(match);

        try {
            String[] lines = aiResponse.split("\n");
            BigDecimal homeWin = new BigDecimal("33.33");
            BigDecimal draw = new BigDecimal("33.34");
            BigDecimal awayWin = new BigDecimal("33.33");
            String reasoning = aiResponse;

            for (String line : lines) {
                line = line.trim();
                if (line.toUpperCase().startsWith("HOME_WIN: ")) {
                    String value = line.substring(9).trim().replace("%", "").trim();
                    homeWin = new BigDecimal(value);
                } else if (line.toUpperCase().startsWith("DRAW:")) {
                    String value = line.substring(5).trim().replace("%", "").trim();
                    draw = new BigDecimal(value);
                } else if (line. toUpperCase().startsWith("AWAY_WIN:")) {
                    String value = line.substring(9).trim().replace("%", "").trim();
                    awayWin = new BigDecimal(value);
                } else if (line.toUpperCase().startsWith("REASONING:")) {
                    reasoning = line.substring(10).trim();
                }
            }

            prediction.setHomeWinProbability(homeWin);
            prediction.setDrawProbability(draw);
            prediction.setAwayWinProbability(awayWin);
            prediction.setReasoning(reasoning);
            prediction.setConfidenceScore(new BigDecimal("0.75"));

        } catch (Exception e) {
            prediction.setHomeWinProbability(new BigDecimal("33.33"));
            prediction.setDrawProbability(new BigDecimal("33.34"));
            prediction.setAwayWinProbability(new BigDecimal("33.33"));
            prediction.setReasoning("AI analysis:  " + aiResponse);
            prediction.setConfidenceScore(new BigDecimal("0.50"));
        }

        return prediction;
    }

    //Get conversation history for a session
    public List<ConversationContext> getConversationHistory(String sessionId) {
        return contextRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

}
