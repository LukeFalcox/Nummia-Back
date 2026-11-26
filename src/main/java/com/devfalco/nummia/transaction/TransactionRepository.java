package com.devfalco.nummia.transaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    @EntityGraph(attributePaths = {"account", "destinationAccount", "category"})
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByAccountIdOrDestinationAccountId(UUID accountId, UUID destinationAccountId);

    /** Soma com sinal (receita +, despesa -, transferência enviada -) agrupada pela conta de origem. */
    @Query("""
            select t.account.id, sum(case
                when t.type = com.devfalco.nummia.transaction.TransactionType.INCOME then t.amount
                else -t.amount end)
            from Transaction t
            where t.userId = :userId and t.date <= :until
            group by t.account.id""")
    List<Object[]> signedTotalsByAccount(UUID userId, LocalDate until);

    /** Transferências recebidas agrupadas pela conta de destino. */
    @Query("""
            select t.destinationAccount.id, sum(t.amount)
            from Transaction t
            where t.userId = :userId and t.type = com.devfalco.nummia.transaction.TransactionType.TRANSFER
              and t.date <= :until
            group by t.destinationAccount.id""")
    List<Object[]> incomingTransfersByAccount(UUID userId, LocalDate until);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.type = :type and t.date between :from and :to""")
    BigDecimal sumByType(UUID userId, TransactionType type, LocalDate from, LocalDate to);

    @Query("""
            select new com.devfalco.nummia.transaction.CategoryTotal(c.id, c.name, c.color, c.icon, sum(t.amount))
            from Transaction t join t.category c
            where t.userId = :userId and t.type = :type and t.date between :from and :to
            group by c.id, c.name, c.color, c.icon
            order by sum(t.amount) desc""")
    List<CategoryTotal> totalsByCategory(UUID userId, TransactionType type, LocalDate from, LocalDate to);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.userId = :userId and t.category.id = :categoryId
              and t.type = com.devfalco.nummia.transaction.TransactionType.EXPENSE
              and t.date between :from and :to""")
    BigDecimal sumExpensesByCategory(UUID userId, UUID categoryId, LocalDate from, LocalDate to);

    /** Totais mensais: [ano, mês, tipo, soma]. */
    @Query("""
            select year(t.date), month(t.date), t.type, sum(t.amount)
            from Transaction t
            where t.userId = :userId and t.type <> com.devfalco.nummia.transaction.TransactionType.TRANSFER
              and t.date between :from and :to
            group by year(t.date), month(t.date), t.type""")
    List<Object[]> monthlyTotals(UUID userId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"account", "category"})
    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.type = :type and t.date between :from and :to
            order by t.date desc""")
    List<Transaction> findByTypeAndPeriod(UUID userId, TransactionType type, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"account", "destinationAccount", "category"})
    @Query("select t from Transaction t where t.userId = :userId order by t.date desc, t.createdAt desc")
    List<Transaction> findRecent(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
            select t from Transaction t
            where t.userId = :userId and t.recurring = true
              and t.type = com.devfalco.nummia.transaction.TransactionType.EXPENSE
              and t.date between :from and :to""")
    List<Transaction> findRecurringExpenses(UUID userId, LocalDate from, LocalDate to);
}
