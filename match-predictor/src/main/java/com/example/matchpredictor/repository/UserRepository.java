package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>{

    //Find user by username (for authentification)
    Optional<User> findByUsername(String username);

    //Find user by email
    Optional<User> findByEmail(String email);

    //Check if username exists
    boolean existsByUsername(String username);

    //Check if email exists
    boolean existsByEmail(String email);

    //Find user by role
    @Query("SELECT u FROM User u WHERE u.role = :role")
    java.util.List<User> findByRole(String role);

    // Find enabled users only
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    java.util.List<User> findEnabledUsers();
}
