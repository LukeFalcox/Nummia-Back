package com.devfalco.nummia.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, UUID> {

    List<GoalContribution> findByGoalIdOrderByDateDescCreatedAtDesc(UUID goalId);
}
