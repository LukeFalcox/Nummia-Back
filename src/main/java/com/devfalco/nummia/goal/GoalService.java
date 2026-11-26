package com.devfalco.nummia.goal;

import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.common.exception.BusinessException;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.goal.dto.ContributionRequest;
import com.devfalco.nummia.goal.dto.ContributionResponse;
import com.devfalco.nummia.goal.dto.GoalRequest;
import com.devfalco.nummia.goal.dto.GoalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository repository;
    private final GoalContributionRepository contributions;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<GoalResponse> list(UUID userId, GoalStatus status) {
        List<Goal> goals = status == null
                ? repository.findByUserIdOrderByCreatedAtDesc(userId)
                : repository.findByUserIdAndStatusOrderByDeadlineAsc(userId, status);
        return goals.stream().map(GoalService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GoalResponse find(UUID userId, UUID id) {
        return toResponse(get(userId, id));
    }

    @Transactional
    public GoalResponse create(UUID userId, GoalRequest req) {
        validateDeadline(req.deadline());
        Goal g = new Goal();
        g.setUserId(userId);
        apply(g, req);
        g.setCurrentAmount(Money.of(req.initialAmount()));
        updateCompletion(g);
        repository.save(g);
        if (g.getCurrentAmount().signum() > 0) {
            addContribution(g, g.getCurrentAmount(), LocalDate.now(), "Valor inicial");
        }
        return toResponse(g);
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID id, GoalRequest req) {
        Goal g = get(userId, id);
        apply(g, req);
        if (req.status() != null) g.setStatus(req.status());
        if (g.getStatus() != GoalStatus.CANCELLED) updateCompletion(g);
        return toResponse(g);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(get(userId, id));
    }

    @Transactional
    public GoalResponse contribute(UUID userId, UUID id, ContributionRequest req) {
        Goal g = get(userId, id);
        if (g.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessException("Não é possível movimentar uma meta cancelada.");
        }
        if (req.amount().signum() == 0) {
            throw new BusinessException("O valor do aporte não pode ser zero.");
        }
        BigDecimal newAmount = g.getCurrentAmount().add(req.amount());
        if (newAmount.signum() < 0) {
            throw new BusinessException("O resgate é maior que o valor acumulado na meta.");
        }
        g.setCurrentAmount(Money.of(newAmount));
        updateCompletion(g);
        addContribution(g, Money.of(req.amount()), req.date() != null ? req.date() : LocalDate.now(), req.note());
        events.publishEvent(new GoalProgressEvent(userId, g.getId()));
        return toResponse(g);
    }

    @Transactional(readOnly = true)
    public List<ContributionResponse> contributions(UUID userId, UUID id) {
        Goal g = get(userId, id);
        return contributions.findByGoalIdOrderByDateDescCreatedAtDesc(g.getId()).stream()
                .map(ContributionResponse::from).toList();
    }

    public Goal get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Meta"));
    }

    public static GoalResponse toResponse(Goal g) {
        BigDecimal remaining = g.getTargetAmount().subtract(g.getCurrentAmount()).max(BigDecimal.ZERO);
        Long months = null;
        BigDecimal monthly = null;
        if (g.getDeadline() != null && g.getStatus() == GoalStatus.ACTIVE) {
            months = Math.max(0, ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(g.getDeadline())));
            long divisor = Math.max(1, months);
            monthly = remaining.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_EVEN);
        }
        return new GoalResponse(g.getId(), g.getName(), g.getDescription(), g.getTargetAmount(), g.getCurrentAmount(),
                Money.of(remaining), Money.percentage(g.getCurrentAmount(), g.getTargetAmount()), g.getDeadline(),
                months, monthly, g.getStatus(), g.getIcon(), g.getColor());
    }

    private void addContribution(Goal g, BigDecimal amount, LocalDate date, String note) {
        GoalContribution c = new GoalContribution();
        c.setGoalId(g.getId());
        c.setAmount(amount);
        c.setDate(date);
        c.setNote(note);
        c.setCreatedAt(Instant.now());
        contributions.save(c);
    }

    private static void updateCompletion(Goal g) {
        if (g.getCurrentAmount().compareTo(g.getTargetAmount()) >= 0) {
            g.setStatus(GoalStatus.COMPLETED);
        } else if (g.getStatus() == GoalStatus.COMPLETED) {
            g.setStatus(GoalStatus.ACTIVE);
        }
    }

    private static void validateDeadline(LocalDate deadline) {
        if (deadline != null && deadline.isBefore(LocalDate.now())) {
            throw new BusinessException("O prazo da meta não pode estar no passado.");
        }
    }

    private static void apply(Goal g, GoalRequest req) {
        g.setName(req.name().trim());
        g.setDescription(req.description());
        g.setTargetAmount(Money.of(req.targetAmount()));
        g.setDeadline(req.deadline());
        g.setIcon(req.icon());
        g.setColor(req.color());
    }
}
