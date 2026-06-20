package com.example.matchpredictor.service;

import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity. Team;
import com.example. matchpredictor.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DataInitializationService {

    @Autowired
    private TeamService teamService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserService userService;

    public String initializeSampleData() {
        try {
            // Create teams if they don't exist
            Team manUtd = getOrCreateTeam("Manchester United", "England", "https://example.com/mu-logo.png", 1878);
            Team liverpool = getOrCreateTeam("Liverpool", "England", "https://example.com/liverpool-logo.png", 1892);
            Team realMadrid = getOrCreateTeam("Real Madrid", "Spain", "https://example.com/rm-logo.png", 1902);
            Team barcelona = getOrCreateTeam("Barcelona", "Spain", "https://example.com/barca-logo. png", 1899);

            // Create future matches for predictions
            createMatchIfNotExists(manUtd, liverpool, LocalDateTime.now().plusDays(7), "Premier League", "Old Trafford");
            createMatchIfNotExists(realMadrid, barcelona, LocalDateTime.now().plusDays(10), "La Liga", "Santiago Bernabéu");
            createMatchIfNotExists(liverpool, realMadrid, LocalDateTime.now().plusDays(14), "Champions League", "Anfield");

            return "Sample data initialized successfully!\n" +
                    "Teams: " + teamService.getTeamCount() + "\n" +
                    "Matches: " + matchService.getMatchCount() + "\n" +
                    "\nTest endpoints:\n" +
                    "- GET /api/predictions/upcoming-matches\n" +
                    "- POST /api/predictions/generate/{matchId}\n" +
                    "- GET /api/predictions/match/{matchId}/latest";

        } catch (Exception e) {
            return "Error initializing data: " + e.getMessage();
        }
    }

    private Team getOrCreateTeam(String name, String country, String logo, Integer foundedYear) {
        return teamService.getTeamByName(name). orElseGet(() -> {
            Team team = new Team(name, country);
            team.setLogo(logo);
            team.setFoundedYear(foundedYear);
            return teamService.createTeam(team);
        });
    }

    private Match createMatchIfNotExists(Team homeTeam, Team awayTeam, LocalDateTime matchDate, String league, String venue) {
        // Simple check - you can enhance this
        Match match = new Match(homeTeam, awayTeam, matchDate, league);
        match.setVenue(venue);
        match.setStatus("SCHEDULED");
        return matchService.createMatch(match);
    }
}
