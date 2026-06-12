package com.javaweb.ai.repository;

import com.javaweb.ai.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {
}
