package com.javaweb.ai.repository;

import com.javaweb.ai.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {
}
