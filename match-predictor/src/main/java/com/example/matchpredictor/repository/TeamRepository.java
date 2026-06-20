package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {

    // Find team by name
    Optional<Team> findByName(String name);

    // Find teams by country
    List<Team> findByCountry(String country);

    // Find teams by name containing (search)
    @Query("SELECT t FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Team> findByNameContainingIgnoreCase(String name);

    // Find teams ordered by name
    @Query("SELECT t FROM Team t ORDER BY t.name ASC")
    List<Team> findAllOrderByName();

    // Check if team name exists
    boolean existsByName(String name);
}
