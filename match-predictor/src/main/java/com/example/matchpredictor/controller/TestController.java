package com.example. matchpredictor.controller;

import com.example.matchpredictor. service.DataInitializationService;
import org.springframework.ai.ollama.OllamaChatClient;
import org. springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaApi;
import org. springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation. GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework. web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private OllamaChatClient ollamaChatClient;

    @Autowired
    private DataInitializationService dataInitializationService;

    @Value("${spring. ai.ollama.chat.model:NOT_SET}")
    private String configuredModel;

    @GetMapping("/debug-config")
    public String debugConfig() {
        return """
                DEBUG CONFIGURATION:
                
                Model from application.properties: %s
                OllamaChatClient class: %s
                
                Test /test-ai-detailed for more info
                """.formatted(configuredModel, ollamaChatClient. getClass().getName());
    }

    @GetMapping("/test-ai-detailed")
    public String testAiDetailed(@RequestParam(defaultValue = "Hello") String message) {
        System.out.println("DEBUG: Starting test-ai-detailed");
        System.out.println("DEBUG: Configured model: " + configuredModel);
        System.out.println("DEBUG: Message: " + message);
        System.out.println("DEBUG: OllamaChatClient instance: " + ollamaChatClient);

        try {
            System.out.println("DEBUG: About to create Prompt");
            Prompt prompt = new Prompt(message);
            System.out. println("DEBUG: Prompt created successfully");

            System.out.println("DEBUG: About to call ollamaChatClient.call()");
            ChatResponse response = ollamaChatClient.call(prompt);
            System.out.println("DEBUG: Call successful, got response");

            String result = response.getResult().getOutput(). getContent();
            System.out. println("DEBUG: Extracted content from response");

            return "AI Response:\n\n" + result;

        } catch (Exception e) {
            System.err.println("DEBUG: Exception occurred");
            System.err.println("DEBUG: Exception type: " + e.getClass().getName());
            System.err.println("DEBUG: Exception message: " + e.getMessage());
            System.err.println("DEBUG: Stack trace:");
            e.printStackTrace();

            return "DETAILED ERROR:\n" +
                    "Exception Type: " + e.getClass(). getSimpleName() + "\n" +
                    "Message: " + e.getMessage() + "\n" +
                    "Configured Model: " + configuredModel + "\n" +
                    "Client Class: " + ollamaChatClient.getClass().getName() + "\n" +
                    "\nCheck console for full stack trace! ";
        }
    }

    // Keep your existing methods...
    @GetMapping("/")
    public String home() {
        return """
                Match Prediction Assistant is running!
                
                DEBUG Endpoints:
                - GET /debug-config (Show configuration)
                - GET /test-ai-detailed? message=Hello (Detailed AI test)
                
                API Endpoints:
                - GET /initialize-data (Create sample teams & matches)
                - GET /api/predictions/teams (List all teams)
                - GET /api/predictions/upcoming-matches (Get matches for prediction)
                
                AI Predictions:
                - POST /api/predictions/generate/{matchId} (Generate AI prediction)
                - GET /api/predictions/match/{matchId}/latest (Get latest prediction)
                - GET /api/predictions/stats (Prediction accuracy)
                """;
    }

    @GetMapping("/initialize-data")
    public String initializeData() {
        return dataInitializationService.initializeSampleData();
    }

    @GetMapping("/test-ai")
    public String testAi(@RequestParam(defaultValue = "Hello") String message) {
        try {
            // Create a NEW client with explicit model each time
            OllamaApi directApi = new OllamaApi("http://localhost:11434");
            OllamaChatClient directClient = new OllamaChatClient(directApi);

            // Force the model in the options
            ChatResponse response = directClient.call(
                    new Prompt(message,
                            org.springframework.ai.ollama.api.OllamaOptions.create()
                                    .withModel("llama3.2"))
            );

            return "AI Response:\n\n" + response.getResult().getOutput(). getContent();
        } catch (Exception e) {
            return "Error: " + e.getMessage() +
                    "\n\nMake sure Ollama is running with model llama3.2" +
                    "\n\nTry /test-ai-detailed for more debug info";
        }
    }

    @GetMapping("/status")
    public String status() {
        return "Application Status: RUNNING\n" +
                "AI Service: Ollama (llama3.2)\n" +
                "Database: PostgreSQL\n" +
                "Server: http://localhost:8080\n" +
                "Debug: /debug-config, /test-ai-detailed";
    }
}