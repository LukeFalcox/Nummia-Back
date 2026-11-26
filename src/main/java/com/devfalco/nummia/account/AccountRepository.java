package com.devfalco.nummia.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<FinancialAccount, UUID> {

    List<FinancialAccount> findByUserIdOrderByNameAsc(UUID userId);

    List<FinancialAccount> findByUserIdAndArchivedFalseOrderByNameAsc(UUID userId);

    Optional<FinancialAccount> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);
}
