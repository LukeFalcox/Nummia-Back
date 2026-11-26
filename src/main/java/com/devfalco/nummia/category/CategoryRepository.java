package com.devfalco.nummia.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUserIdOrderByTypeAscNameAsc(UUID userId);

    List<Category> findByUserIdAndTypeOrderByNameAsc(UUID userId, CategoryType type);

    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndTypeAndNameIgnoreCase(UUID userId, CategoryType type, String name);

    boolean existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(UUID userId, CategoryType type, String name, UUID id);
}
