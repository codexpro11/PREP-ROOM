package com.substring.docmind.kafka;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AnalysisOutboxRepository extends JpaRepository<AnalysisOutbox, UUID> {
    List<AnalysisOutbox> findTop20ByOrderByCreatedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AnalysisOutbox e where e.id = :id")
    Optional<AnalysisOutbox> lockById(@Param("id") UUID id);
}
