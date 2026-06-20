package com.example.match_predictor.service;


import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.repository.AiPredictionRepository;
import com.example.matchpredictor.repository.MatchRepository;
import com.example.matchpredictor.repository.TeamRepository;
import com.example.matchpredictor.service.AiPredictionService;
import com.example.matchpredictor.service.MatchService;
import com.example.matchpredictor.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.ollama.OllamaChatClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceTestSuite {

    // TEAM SERVICE
    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private TeamService teamService;

    // MATCH SERVICE
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    // AI PREDICTION SERVICE
    @Mock
    private AiPredictionRepository aiPredictionRepository;

    @Mock
    private OllamaChatClient ollamaChatClient;

    @Mock
    private MatchService matchServiceForAI;

    @InjectMocks
    private AiPredictionService aiPredictionService;

    // Test Data
    private Team team1;
    private Team team2;
    private Match match1;
    private AiPrediction prediction1;

    @BeforeEach
    void setUp() {
        // Setup test data
        team1 = new Team("Real Madrid", "Spain");
        team1.setId(1);
        team1.setFoundedYear(1902);

        team2 = new Team("Barcelona", "Spain");
        team2.setId(2);
        team2.setFoundedYear(1899);

        match1 = new Match(team1, team2, LocalDateTime.now().plusDays(1), "La Liga");
        match1.setId(1);
        match1.setVenue("Santiago Bernabéu");

        prediction1 = new AiPrediction();
        prediction1.setMatch(match1);
        prediction1.setReasoning("Real Madrid has home advantage");
    }

    // TEAM SERVICE TESTS

    @Test
    void teamService_getAllTeams_ShouldReturnListOfTeams() {
        List<Team> expectedTeams = Arrays.asList(team1, team2);
        when(teamRepository.findAllOrderByName()).thenReturn(expectedTeams);

        List<Team> actualTeams = teamService.getAllTeams();

        assertEquals(2, actualTeams.size());
        assertEquals("Real Madrid", actualTeams.get(0).getName());
        verify(teamRepository, times(1)).findAllOrderByName();
    }

    @Test
    void teamService_getTeamById_ShouldReturnTeam_WhenExists() {
        when(teamRepository.findById(1)).thenReturn(Optional.of(team1));

        Optional<Team> result = teamService.getTeamById(1);

        assertTrue(result.isPresent());
        assertEquals("Real Madrid", result.get().getName());
        verify(teamRepository, times(1)).findById(1);
    }

    @Test
    void teamService_createTeam_ShouldThrowException_WhenNameExists() {
        when(teamRepository.existsByName("Real Madrid")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            teamService.createTeam(team1);
        });

        assertEquals("Team with name 'Real Madrid' already exists", exception.getMessage());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void teamService_getTeamsByCountry_ShouldReturnTeamsFromCountry() {
        List<Team> spanishTeams = Arrays.asList(team1, team2);
        when(teamRepository.findByCountry("Spain")).thenReturn(spanishTeams);

        List<Team> result = teamService.getTeamsByCountry("Spain");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(team -> "Spain".equals(team.getCountry())));
        verify(teamRepository, times(1)).findByCountry("Spain");
    }

    //MATCH SERVICE TESTS

    @Test
    void matchService_getUpcomingMatches_ShouldReturnFutureMatches() {
        List<Match> expectedMatches = Arrays.asList(match1);
        when(matchRepository.findUpcomingMatches(any(LocalDateTime.class)))
                .thenReturn(expectedMatches);

        List<Match> actualMatches = matchService.getUpcomingMatches();

        assertEquals(1, actualMatches.size());
        assertEquals("La Liga", actualMatches.get(0).getLeague());
        verify(matchRepository, times(1)).findUpcomingMatches(any(LocalDateTime.class));
    }

    @Test
    void matchService_getMatchById_ShouldReturnMatch_WhenExists() {
        when(matchRepository.findById(1)).thenReturn(Optional.of(match1));

        Optional<Match> result = matchService.getMatchById(1);

        assertTrue(result.isPresent());
        assertEquals("Real Madrid", result.get().getHomeTeam().getName());
        verify(matchRepository, times(1)).findById(1);
    }

    @Test
    void matchService_createMatch_ShouldThrowException_WhenSameTeams() {
        Match invalidMatch = new Match(team1, team1, LocalDateTime.now().plusDays(1), "La Liga");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            matchService.createMatch(invalidMatch);
        });

        assertEquals("Home team and away team must be different", exception.getMessage());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void matchService_getAllMatches_ShouldReturnAllMatches() {
        List<Match> allMatches = Arrays.asList(match1);
        when(matchRepository.findAll()).thenReturn(allMatches);

        List<Match> result = matchService.getAllMatches();

        assertEquals(1, result.size());
        assertEquals("La Liga", result.get(0).getLeague());
        verify(matchRepository, times(1)).findAll();
    }

    // ==================== AI PREDICTION SERVICE TESTS ====================

    @Test
    void aiPredictionService_generatePrediction_ShouldThrowException_WhenMatchNotFound() {
        // Use matchServiceForAI instead of matchService
        when(matchServiceForAI.getMatchById(999)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiPredictionService.generatePrediction(999);
        });

        assertEquals("Match not found with id: 999", exception.getMessage());
        verify(aiPredictionRepository, never()).save(any());
    }

    @Test
    void aiPredictionService_getPredictionsForMatch_ShouldReturnPredictions() {
        List<AiPrediction> predictions = Arrays.asList(prediction1);
        when(matchServiceForAI.getMatchById(1)).thenReturn(Optional.of(match1));
        when(aiPredictionRepository.findByMatch(match1)).thenReturn(predictions);

        List<AiPrediction> result = aiPredictionService.getPredictionsForMatch(1);

        assertEquals(1, result.size());
        assertEquals(match1, result.get(0).getMatch());
        verify(aiPredictionRepository, times(1)).findByMatch(match1);
    }

    @Test
    void aiPredictionService_getAllPredictions_ShouldReturnAllPredictions() {
        List<AiPrediction> allPredictions = Arrays.asList(prediction1);
        when(aiPredictionRepository.findAll()).thenReturn(allPredictions);

        List<AiPrediction> result = aiPredictionService.getAllPredictions();

        assertEquals(1, result.size());
        assertEquals("Real Madrid has home advantage", result.get(0).getReasoning());
        verify(aiPredictionRepository, times(1)).findAll();
    }
}
