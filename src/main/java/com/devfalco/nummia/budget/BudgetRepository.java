package com.devfalco.nummia.budget;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserIdAndYearAndMonth(UUID userId, int year, int month);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = "category")
    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(UUID userId, UUID categoryId, int year, int month);

    boolean existsByUserIdAndCategoryIdAndYearAndMonth(UUID userId, UUID categoryId, int year, int month);

    @Modifying
    @Query("delete from Budget b where b.category.id = :categoryId")
    void deleteByCategoryId(UUID categoryId);
}
