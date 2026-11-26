package com.devfalco.nummia.budget;

import com.devfalco.nummia.category.Category;
import com.devfalco.nummia.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/** Limite mensal de gastos para uma categoria de despesa. */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
public class Budget extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "budget_year", nullable = false)
    private int year;

    @Column(name = "budget_month", nullable = false)
    private int month;

    @Column(name = "limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    /** Percentual de uso a partir do qual o usuário é alertado. */
    @Column(name = "alert_threshold", nullable = false)
    private int alertThreshold = 80;

    public YearMonth period() {
        return YearMonth.of(year, month);
    }
}
