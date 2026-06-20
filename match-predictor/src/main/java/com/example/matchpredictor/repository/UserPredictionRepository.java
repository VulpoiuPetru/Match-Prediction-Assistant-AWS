package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity.User;
import com.example.matchpredictor.entity.UserPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPredictionRepository extends JpaRepository<UserPrediction, Integer>{

    // Find predictions by user
    List<UserPrediction> findByUser(User user);

    // Find predictions by user ordered by date
    @Query("SELECT up FROM UserPrediction up WHERE up.user = :user ORDER BY up.createdAt DESC")
    List<UserPrediction> findByUserOrderByCreatedAtDesc(User user);

    // Find predictions by session
    List<UserPrediction> findBySessionId(String sessionId);

    // Find recent predictions by user - CORRECTED (using Pageable)
    @Query("SELECT up FROM UserPrediction up WHERE up.user = :user ORDER BY up.createdAt DESC")
    List<UserPrediction> findRecentByUser(User user, Pageable pageable);

    // Search predictions by prompt content
    @Query("SELECT up FROM UserPrediction up WHERE LOWER(up.prompt) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<UserPrediction> searchByPrompt(String searchTerm);

    // Count predictions by user
    @Query("SELECT COUNT(up) FROM UserPrediction up WHERE up.user = :user")
    Long countByUser(User user);
}
