package com.substring.docmind.interview;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {
    List<Interview> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Interview> findByIdAndUserId(UUID id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Interview i where i.id = :id and i.userId = :userId")
    Optional<Interview> lockOwned(@Param("id") UUID id, @Param("userId") Long userId);
}
