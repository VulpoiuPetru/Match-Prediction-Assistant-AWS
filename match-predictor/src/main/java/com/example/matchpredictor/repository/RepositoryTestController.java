package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.entity.User;
import com.example.matchpredictor.repository.TeamRepository;
import com.example.matchpredictor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-db")
public class RepositoryTestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @GetMapping("/create-sample-data")
    public String createSampleData() {
        try {
            // Create sample user
            if (!userRepository.existsByUsername("testuser")) {
                User user = new User("testuser", "password123", "test@example.com", "USER");
                userRepository.save(user);
            }

            // Create sample teams
            if (!teamRepository.existsByName("Manchester United")) {
                Team team1 = new Team("Manchester United", "England");
                team1.setLogo("https://example.com/mu-logo.png");
                team1.setFoundedYear(1878);
                teamRepository.save(team1);
            }

            if (!teamRepository.existsByName("Liverpool")) {
                Team team2 = new Team("Liverpool", "England");
                team2.setLogo("https://example.com/liverpool-logo.png");
                team2.setFoundedYear(1892);
                teamRepository.save(team2);
            }

            return "Sample data created successfully!\n\n" +
                    "Users: " + userRepository.count() + "\n" +
                    "Teams: " + teamRepository.count();

        } catch (Exception e) {
            return "Error creating sample data: " + e.getMessage();
        }
    }

    @GetMapping("/count-all")
    public String countAll() {
        return "Database Statistics:\n\n" +
                "Users: " + userRepository.count() + "\n" +
                "Teams: " + teamRepository.count() + "\n" +
                "Matches: 0 (none created yet)\n" +
                "AI Predictions: 0\n" +
                "User Predictions: 0";
    }
}
