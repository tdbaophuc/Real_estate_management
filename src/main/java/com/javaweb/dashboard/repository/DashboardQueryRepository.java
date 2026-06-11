package com.javaweb.dashboard.repository;

import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.dashboard.dto.CurrencyAmountResponse;
import com.javaweb.dashboard.dto.RevenueCurrencyResponse;
import com.javaweb.dashboard.dto.StatusCountResponse;
import com.javaweb.dashboard.dto.TopAgentResponse;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.payment.enums.PaymentStatus;
import com.javaweb.transaction.enums.DepositStatus;
import com.javaweb.transaction.enums.TransactionStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardQueryRepository {
    private static final List<TransactionStatus> ACTIVE_TRANSACTION_STATUSES = List.of(
            TransactionStatus.PENDING,
            TransactionStatus.DEPOSITED,
            TransactionStatus.CONTRACT_SIGNED,
            TransactionStatus.PAYMENT_IN_PROGRESS
    );
    private static final List<FollowUpTaskStatus> OPEN_TASK_STATUSES = List.of(
            FollowUpTaskStatus.PENDING,
            FollowUpTaskStatus.IN_PROGRESS
    );
    private static final List<AppointmentStatus> ACTIVE_APPOINTMENT_STATUSES = List.of(
            AppointmentStatus.PENDING,
            AppointmentStatus.CONFIRMED
    );

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
        return statusCounts(LeadPipelineStatus.class, rows);
    }

    public List<StatusCountResponse> leadCounts(Long agentId) {
        List<Object[]> rows = entityManager.createQuery("""
                select lead.status, count(lead)
                from Lead lead
                where lead.deletedAt is null
                  and lead.currentAssignee.id = :agentId
                group by lead.status
                """, Object[].class)
                .setParameter("agentId", agentId)
                .getResultList();
        return statusCounts(LeadPipelineStatus.class, rows);
    }

    public List<StatusCountResponse> leadCounts(
            Instant start,
            Instant endExclusive
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                select lead.status, count(lead)
                from Lead lead
                where lead.deletedAt is null
                  and lead.createdAt >= :start
                  and lead.createdAt < :end
                group by lead.status
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", endExclusive)
                .getResultList();
        return statusCounts(LeadPipelineStatus.class, rows);
    }

    public List<StatusCountResponse> transactionCounts() {
        List<Object[]> rows = entityManager.createQuery("""
                select transaction.status, count(transaction)
                from Transaction transaction
                group by transaction.status
                """, Object[].class).getResultList();
        return statusCounts(TransactionStatus.class, rows);
    }

    public List<StatusCountResponse> transactionCounts(
            Instant start,
            Instant endExclusive
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                select transaction.status, count(transaction)
                from Transaction transaction
                where transaction.createdAt >= :start
                  and transaction.createdAt < :end
                group by transaction.status
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", endExclusive)
                .getResultList();
        return statusCounts(TransactionStatus.class, rows);
    }

    public List<StatusCountResponse> activeTransactionCounts(Long agentId) {
        List<Object[]> rows = entityManager.createQuery("""
                select transaction.status, count(transaction)
                from Transaction transaction
                where transaction.agent.id = :agentId
                  and transaction.status in :statuses
                group by transaction.status
                """, Object[].class)
                .setParameter("agentId", agentId)
                .setParameter("statuses", ACTIVE_TRANSACTION_STATUSES)
                .getResultList();
        Map<TransactionStatus, Long> counts =
                enumCountMap(TransactionStatus.class, rows);
        return ACTIVE_TRANSACTION_STATUSES.stream()
                .map(status -> new StatusCountResponse(
                        status.name(),
                        counts.getOrDefault(status, 0L)
                ))
                .toList();
    }

    public List<RevenueCurrencyResponse> revenueSummary() {
        return revenueSummary(null, null);
    }

    public List<RevenueCurrencyResponse> revenueSummary(
            Instant start,
            Instant endExclusive
    ) {
        Map<String, RevenueAccumulator> currencies = new LinkedHashMap<>();
        String transactionRange = start == null
                ? ""
                : " and transaction.completedAt >= :start"
                + " and transaction.completedAt < :end\n";
        var transactionQuery = entityManager.createQuery("""
                select transaction.currency,
                       count(transaction),
                       sum(transaction.agreedValue)
                from Transaction transaction
                where transaction.status = :status
                """ + transactionRange + """
                group by transaction.currency
                """, Object[].class)
                .setParameter("status", TransactionStatus.COMPLETED);
        setRange(transactionQuery, start, endExclusive);
        transactionQuery.getResultList()
                .forEach(row -> accumulator(currencies, (String) row[0])
                        .setCompletedTransactions(
                                (Long) row[1],
                                (BigDecimal) row[2]
                        ));
        String paymentRange = start == null
                ? ""
                : " and payment.paidAt >= :start and payment.paidAt < :end\n";
        var paymentQuery = entityManager.createQuery("""
                select payment.currency, sum(payment.amount)
                from Payment payment
                where payment.status = :status
                """ + paymentRange + """
                group by payment.currency
                """, Object[].class)
                .setParameter("status", PaymentStatus.COMPLETED);
        setRange(paymentQuery, start, endExclusive);
        addAmounts(
                currencies,
                paymentQuery.getResultList(),
                RevenueAccumulator::setCompletedPayments
        );
        String depositRange = start == null
                ? ""
                : " and deposit.verifiedAt >= :start and deposit.verifiedAt < :end\n";
        var depositQuery = entityManager.createQuery("""
                select deposit.currency, sum(deposit.amount)
                from Deposit deposit
                where deposit.status = :status
                """ + depositRange + """
                group by deposit.currency
                """, Object[].class)
                .setParameter("status", DepositStatus.VERIFIED);
        setRange(depositQuery, start, endExclusive);
        addAmounts(
                currencies,
                depositQuery.getResultList(),
                RevenueAccumulator::setVerifiedDeposits
        );
        String commissionRange = start == null
                ? ""
                : " and commission.paidAt >= :start and commission.paidAt < :end\n";
        var commissionQuery = entityManager.createQuery("""
                select commission.currency, sum(commission.amount)
                from Commission commission
                where commission.status = :status
                """ + commissionRange + """
                group by commission.currency
                """, Object[].class)
                .setParameter("status", CommissionStatus.PAID);
        setRange(commissionQuery, start, endExclusive);
        addAmounts(
                currencies,
                commissionQuery.getResultList(),
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

    public long countTodayAppointments(
            Long agentId,
            Instant start,
            Instant endExclusive
    ) {
        return entityManager.createQuery("""
                select count(appointment)
                from Appointment appointment
                where appointment.agent.id = :agentId
                  and appointment.status in :statuses
                  and appointment.startAt >= :start
                  and appointment.startAt < :end
                """, Long.class)
                .setParameter("agentId", agentId)
                .setParameter("statuses", ACTIVE_APPOINTMENT_STATUSES)
                .setParameter("start", start)
                .setParameter("end", endExclusive)
                .getSingleResult();
    }

    public long countOpenFollowUpTasks(Long agentId) {
        return entityManager.createQuery("""
                select count(task)
                from FollowUpTask task
                where task.assignedTo.id = :agentId
                  and task.status in :statuses
                """, Long.class)
                .setParameter("agentId", agentId)
                .setParameter("statuses", OPEN_TASK_STATUSES)
                .getSingleResult();
    }

    public long countOverdueFollowUpTasks(Long agentId, Instant now) {
        return entityManager.createQuery("""
                select count(task)
                from FollowUpTask task
                where task.assignedTo.id = :agentId
                  and task.status in :statuses
                  and task.dueAt < :now
                """, Long.class)
                .setParameter("agentId", agentId)
                .setParameter("statuses", OPEN_TASK_STATUSES)
                .setParameter("now", now)
                .getSingleResult();
    }

    public List<StatusCountResponse> commissionCounts(Long beneficiaryUserId) {
        List<Object[]> rows = entityManager.createQuery("""
                select commission.status, count(commission)
                from Commission commission
                where commission.beneficiaryUser.id = :beneficiaryUserId
                group by commission.status
                """, Object[].class)
                .setParameter("beneficiaryUserId", beneficiaryUserId)
                .getResultList();
        return statusCounts(CommissionStatus.class, rows);
    }

    public List<StatusCountResponse> commissionCounts(
            Instant start,
            Instant endExclusive
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                select commission.status, count(commission)
                from Commission commission
                where commission.createdAt >= :start
                  and commission.createdAt < :end
                group by commission.status
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", endExclusive)
                .getResultList();
        return statusCounts(CommissionStatus.class, rows);
    }

    public List<CurrencyAmountResponse> commissionAmounts(Long beneficiaryUserId) {
        return entityManager.createQuery("""
                select commission.currency, count(commission), sum(commission.amount)
                from Commission commission
                where commission.beneficiaryUser.id = :beneficiaryUserId
                group by commission.currency
                order by commission.currency
                """, Object[].class)
                .setParameter("beneficiaryUserId", beneficiaryUserId)
                .getResultList()
                .stream()
                .map(this::currencyAmount)
                .toList();
    }

    public List<CurrencyAmountResponse> commissionAmounts(
            Instant start,
            Instant endExclusive
    ) {
        return entityManager.createQuery("""
                select commission.currency, count(commission), sum(commission.amount)
                from Commission commission
                where commission.createdAt >= :start
                  and commission.createdAt < :end
                group by commission.currency
                order by commission.currency
                """, Object[].class)
                .setParameter("start", start)
                .setParameter("end", endExclusive)
                .getResultList()
                .stream()
                .map(this::currencyAmount)
                .toList();
    }

    public List<CurrencyAmountResponse> completedTransactionValues(
            Instant start,
            Instant endExclusive
    ) {
        return entityManager.createQuery("""
                select transaction.currency,
                       count(transaction),
                       sum(transaction.agreedValue)
                from Transaction transaction
                where transaction.status = :status
                  and transaction.completedAt >= :start
                  and transaction.completedAt < :end
                group by transaction.currency
                order by transaction.currency
                """, Object[].class)
                .setParameter("status", TransactionStatus.COMPLETED)
                .setParameter("start", start)
                .setParameter("end", endExclusive)
                .getResultList()
                .stream()
                .map(this::currencyAmount)
                .toList();
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

    private <E extends Enum<E>> List<StatusCountResponse> statusCounts(
            Class<E> enumType,
            List<Object[]> rows
    ) {
        Map<E, Long> counts = enumCountMap(enumType, rows);
        return Arrays.stream(enumType.getEnumConstants())
                .map(status -> new StatusCountResponse(
                        status.name(),
                        counts.getOrDefault(status, 0L)
                ))
                .toList();
    }

    private <E extends Enum<E>> Map<E, Long> enumCountMap(
            Class<E> enumType,
            List<Object[]> rows
    ) {
        Map<E, Long> counts = new EnumMap<>(enumType);
        rows.forEach(row -> counts.put(
                enumType.cast(row[0]),
                ((Number) row[1]).longValue()
        ));
        return counts;
    }

    private CurrencyAmountResponse currencyAmount(Object[] row) {
        return new CurrencyAmountResponse(
                (String) row[0],
                ((Number) row[1]).longValue(),
                (BigDecimal) row[2]
        );
    }

    private void setRange(
            jakarta.persistence.TypedQuery<?> query,
            Instant start,
            Instant endExclusive
    ) {
        if (start != null) {
            query.setParameter("start", start);
            query.setParameter("end", endExclusive);
        }
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
