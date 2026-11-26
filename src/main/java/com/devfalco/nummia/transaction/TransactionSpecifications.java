package com.devfalco.nummia.transaction;

import com.devfalco.nummia.transaction.dto.TransactionFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<Transaction> matching(UUID userId, TransactionFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userId"), userId));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("date"), f.from()));
            if (f.to() != null) p.add(cb.lessThanOrEqualTo(root.get("date"), f.to()));
            if (f.type() != null) p.add(cb.equal(root.get("type"), f.type()));
            if (f.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), f.categoryId()));
            if (f.accountId() != null) {
                p.add(cb.or(
                        cb.equal(root.get("account").get("id"), f.accountId()),
                        cb.equal(root.get("destinationAccount").get("id"), f.accountId())));
            }
            if (f.minAmount() != null) p.add(cb.greaterThanOrEqualTo(root.get("amount"), f.minAmount()));
            if (f.maxAmount() != null) p.add(cb.lessThanOrEqualTo(root.get("amount"), f.maxAmount()));
            if (f.recurring() != null) p.add(cb.equal(root.get("recurring"), f.recurring()));
            if (f.search() != null && !f.search().isBlank()) {
                String like = "%" + f.search().trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.like(cb.lower(root.get("description")), like));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
