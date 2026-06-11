package com.javaweb.audit;

public final class AuditActions {
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_ROLES_CHANGED = "USER_ROLES_CHANGED";
    public static final String LISTING_APPROVED = "LISTING_APPROVED";
    public static final String LISTING_REJECTED = "LISTING_REJECTED";
    public static final String TRANSACTION_STATUS_CHANGED =
            "TRANSACTION_STATUS_CHANGED";
    public static final String CONTRACT_STATUS_CHANGED =
            "CONTRACT_STATUS_CHANGED";
    public static final String COMMISSION_PAID = "COMMISSION_PAID";

    public static final String USER = "USER";
    public static final String LISTING = "LISTING";
    public static final String TRANSACTION = "TRANSACTION";
    public static final String CONTRACT = "CONTRACT";
    public static final String COMMISSION = "COMMISSION";

    private AuditActions() {
    }
}
