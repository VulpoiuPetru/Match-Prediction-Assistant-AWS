package com.example.matchpredictor.repository;

import com.example.matchpredictor.entity. ConversationContext;
import org.springframework.data.domain. Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationContextRepository extends JpaRepository<ConversationContext, Integer> {

    // Get conversation history for a session
    List<ConversationContext> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    // Get recent context for memory (last 5 exchanges)
    @Query("SELECT c FROM ConversationContext c WHERE c.sessionId = : sessionId ORDER BY c.createdAt DESC")
    List<ConversationContext> findRecentContext(String sessionId, Pageable pageable);

    // Count conversations in a session
    long countBySessionId(String sessionId);
}
