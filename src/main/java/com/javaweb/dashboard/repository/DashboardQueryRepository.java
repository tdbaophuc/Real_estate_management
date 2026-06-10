package com.javaweb.dashboard.repository;

import com.javaweb.auth.enums.RoleCode;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.dashboard.dto.RevenueCurrencyResponse;
import com.javaweb.dashboard.dto.StatusCountResponse;
import com.javaweb.dashboard.dto.TopAgentResponse;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.payment.enums.PaymentStatus;
import com.javaweb.transaction.enums.DepositStatus;
import com.javaweb.transaction.enums.TransactionStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardQueryRepository {
    private final EntityManager entityManager;

    public DashboardQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public long countUsers() {
        return entityManager.createQuery(
                "select count(user) from User user",
                Long.class
        ).getSingleResult();
    }

    public long countAgents() {
        return entityManager.createQuery("""
                select count(distinct user)
                from User user
                join user.roles role
                where role.code = :roleCode
                """, Long.class)
                .setParameter("roleCode", RoleCode.AGENT)
                .getSingleResult();
    }

    public long countProperties() {
        return entityManager.createQuery("""
                select count(property)
                from Property property
                where property.deletedAt is null
                """, Long.class).getSingleResult();
    }

    public long countListings() {
        return entityManager.createQuery("""
                select count(listing)
                from Listing listing
                where listing.deletedAt is null
                """, Long.class).getSingleResult();
    }

    public long countPendingListings() {
        return entityManager.createQuery("""
                select count(listing)
                from Listing listing
                where listing.deletedAt is null
                  and listing.status = :status
                """, Long.class)
                .setParameter("status", ListingStatus.PENDING_REVIEW)
                .getSingleResult();
    }

    public List<StatusCountResponse> leadCounts() {
        List<Object[]> rows = entityManager.createQuery("""
                select lead.status, count(lead)
                from Lead lead
                where lead.deletedAt is null
                group by lead.status
                """, Object[].class).getResultList();
        Map<LeadPipelineStatus, Long> counts = new EnumMap<>(LeadPipelineStatus.class);
        rows.forEach(row -> counts.put(
                (LeadPipelineStatus) row[0],
                (Long) row[1]
        ));
        return java.util.Arrays.stream(LeadPipelineStatus.values())
                .map(status -> new StatusCountResponse(
                        status.name(),
                        counts.getOrDefault(status, 0L)
                ))
                .toList();
    }

    public List<StatusCountResponse> transactionCounts() {
        List<Object[]> rows = entityManager.createQuery("""
                select transaction.status, count(transaction)
                from Transaction transaction
                group by transaction.status
                """, Object[].class).getResultList();
        Map<TransactionStatus, Long> counts = new EnumMap<>(TransactionStatus.class);
        rows.forEach(row -> counts.put(
                (TransactionStatus) row[0],
                (Long) row[1]
        ));
        return java.util.Arrays.stream(TransactionStatus.values())
                .map(status -> new StatusCountResponse(
                        status.name(),
                        counts.getOrDefault(status, 0L)
                ))
                .toList();
    }

    public List<RevenueCurrencyResponse> revenueSummary() {
        Map<String, RevenueAccumulator> currencies = new LinkedHashMap<>();
        entityManager.createQuery("""
                select transaction.currency,
                       count(transaction),
                       sum(transaction.agreedValue)
                from Transaction transaction
                where transaction.status = :status
                group by transaction.currency
                """, Object[].class)
                .setParameter("status", TransactionStatus.COMPLETED)
                .getResultList()
                .forEach(row -> accumulator(currencies, (String) row[0])
                        .setCompletedTransactions(
                                (Long) row[1],
                                (BigDecimal) row[2]
                        ));
        addAmounts(
                currencies,
                entityManager.createQuery("""
                        select payment.currency, sum(payment.amount)
                        from Payment payment
                        where payment.status = :status
                        group by payment.currency
                        """, Object[].class)
                        .setParameter("status", PaymentStatus.COMPLETED)
                        .getResultList(),
                RevenueAccumulator::setCompletedPayments
        );
        addAmounts(
                currencies,
                entityManager.createQuery("""
                        select deposit.currency, sum(deposit.amount)
                        from Deposit deposit
                        where deposit.status = :status
                        group by deposit.currency
                        """, Object[].class)
                        .setParameter("status", DepositStatus.VERIFIED)
                        .getResultList(),
                RevenueAccumulator::setVerifiedDeposits
        );
        addAmounts(
                currencies,
                entityManager.createQuery("""
                        select commission.currency, sum(commission.amount)
                        from Commission commission
                        where commission.status = :status
                        group by commission.currency
                        """, Object[].class)
                        .setParameter("status", CommissionStatus.PAID)
                        .getResultList(),
                RevenueAccumulator::setPaidCommissions
        );
        return currencies.values().stream()
                .map(RevenueAccumulator::toResponse)
                .toList();
    }

    public List<TopAgentResponse> topAgents(int limit) {
        return entityManager.createQuery("""
                select transaction.agent.id,
                       transaction.agent.fullName,
                       count(transaction),
                       sum(case when transaction.status = :completed then 1 else 0 end)
                from Transaction transaction
                group by transaction.agent.id, transaction.agent.fullName
                order by sum(case when transaction.status = :completed then 1 else 0 end) desc,
                         count(transaction) desc,
                         transaction.agent.id asc
                """, Object[].class)
                .setParameter("completed", TransactionStatus.COMPLETED)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(row -> new TopAgentResponse(
                        (Long) row[0],
                        (String) row[1],
                        (Long) row[2],
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }

    public long countCommissions(CommissionStatus status) {
        return entityManager.createQuery("""
                select count(commission)
                from Commission commission
                where commission.status = :status
                """, Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    private void addAmounts(
            Map<String, RevenueAccumulator> currencies,
            List<Object[]> rows,
            AmountSetter setter
    ) {
        rows.forEach(row -> setter.set(
                accumulator(currencies, (String) row[0]),
                (BigDecimal) row[1]
        ));
    }

    private RevenueAccumulator accumulator(
            Map<String, RevenueAccumulator> currencies,
            String currency
    ) {
        return currencies.computeIfAbsent(currency, RevenueAccumulator::new);
    }

    @FunctionalInterface
    private interface AmountSetter {
        void set(RevenueAccumulator accumulator, BigDecimal amount);
    }

    private static final class RevenueAccumulator {
        private final String currency;
        private long completedTransactions;
        private BigDecimal completedTransactionValue = BigDecimal.ZERO;
        private BigDecimal completedPayments = BigDecimal.ZERO;
        private BigDecimal verifiedDeposits = BigDecimal.ZERO;
        private BigDecimal paidCommissions = BigDecimal.ZERO;

        private RevenueAccumulator(String currency) {
            this.currency = currency;
        }

        private void setCompletedTransactions(long count, BigDecimal amount) {
            completedTransactions = count;
            completedTransactionValue = zeroIfNull(amount);
        }

        private void setCompletedPayments(BigDecimal amount) {
            completedPayments = zeroIfNull(amount);
        }

        private void setVerifiedDeposits(BigDecimal amount) {
            verifiedDeposits = zeroIfNull(amount);
        }

        private void setPaidCommissions(BigDecimal amount) {
            paidCommissions = zeroIfNull(amount);
        }

        private RevenueCurrencyResponse toResponse() {
            return new RevenueCurrencyResponse(
                    currency,
                    completedTransactions,
                    completedTransactionValue,
                    completedPayments,
                    verifiedDeposits,
                    paidCommissions
            );
        }

        private static BigDecimal zeroIfNull(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}
