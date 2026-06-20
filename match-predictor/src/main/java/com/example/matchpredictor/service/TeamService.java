package com.example.matchpredictor.service;
import com.example.matchpredictor.entity.Team;
import com.example.matchpredictor.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    //Get all teams
    public List<Team> getAllTeams(){
        return teamRepository.findAllOrderByName();
    }

    //Get team by ID
    public Optional<Team> getTeamById(Integer id) {
        return teamRepository.findById(id);
    }

    //Get team by name
    public Optional<Team> getTeamByName(String name) {
        return teamRepository.findByName(name);
    }

    //Get team by country
    public List<Team> getTeamsByCountry(String country) {
        return teamRepository.findByCountry(country);
    }

    // Create new team
    public Team createTeam(Team team) {
        if (teamRepository.existsByName(team.getName())) {
            throw new RuntimeException("Team with name '" + team.getName() + "' already exists");
        }
        return teamRepository.save(team);
    }

    // Update team
    public Team updateTeam(Integer id, Team teamDetails) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));

        team.setName(teamDetails.getName());
        team.setCountry(teamDetails.getCountry());
        team.setLogo(teamDetails.getLogo());
        team.setFoundedYear(teamDetails.getFoundedYear());

        return teamRepository.save(team);
    }

    // Delete team
    public void deleteTeam(Integer id) {
        if (!teamRepository.existsById(id)) {
            throw new RuntimeException("Team not found with id: " + id);
        }
        teamRepository.deleteById(id);
    }

    // Get team count
    public long getTeamCount() {
        return teamRepository.count();
    }
}
