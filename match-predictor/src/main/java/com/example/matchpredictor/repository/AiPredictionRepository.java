package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity.AiPrediction;
import com.example.matchpredictor.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface AiPredictionRepository extends JpaRepository<AiPrediction, Integer> {

    // Find predictions by match
    List<AiPrediction> findByMatch(Match match);

    // Find latest prediction for a match - CORRECTED (using Pageable instead of LIMIT)
    @Query("SELECT ap FROM AiPrediction ap WHERE ap.match = :match ORDER BY ap.createdAt DESC")
    List<AiPrediction> findByMatchOrderByCreatedAtDesc(Match match, Pageable pageable);

    // Find predictions by model version
    List<AiPrediction> findByModelVersion(String modelVersion);

    // Find correct predictions
    @Query("SELECT ap FROM AiPrediction ap WHERE ap.isCorrect = true")
    List<AiPrediction> findCorrectPredictions();

    // Find predictions with high confidence
    @Query("SELECT ap FROM AiPrediction ap WHERE ap.confidenceScore >= :minConfidence ORDER BY ap.confidenceScore DESC")
    List<AiPrediction> findHighConfidencePredictions(java.math.BigDecimal minConfidence);

    // Get accuracy statistics
    @Query("SELECT COUNT(*) FROM AiPrediction ap WHERE ap.isCorrect = true")
    Long countCorrectPredictions();

    @Query("SELECT COUNT(*) FROM AiPrediction ap WHERE ap.isCorrect IS NOT NULL")
    Long countEvaluatedPredictions();

}
