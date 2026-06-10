package com.javaweb.lead.repository;

import com.javaweb.lead.entity.LeadNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadNoteRepository extends JpaRepository<LeadNote, Long> {
    Page<LeadNote> findAllByLeadIdOrderByPinnedDescCreatedAtDesc(
            Long leadId,
            Pageable pageable
    );
}
