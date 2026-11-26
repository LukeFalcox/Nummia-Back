package com.devfalco.nummia.assistant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, UUID> {

    @Query("select m from AssistantMessage m where m.userId = :userId order by m.createdAt desc")
    List<AssistantMessage> findLatest(UUID userId, Pageable pageable);

    @Modifying
    @Query("delete from AssistantMessage m where m.userId = :userId")
    int deleteByUserId(UUID userId);
}
