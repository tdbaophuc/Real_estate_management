package com.javaweb.audit;

public final class AuditActions {
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_ROLES_CHANGED = "USER_ROLES_CHANGED";
    public static final String LISTING_APPROVED = "LISTING_APPROVED";
    public static final String LISTING_REJECTED = "LISTING_REJECTED";
    public static final String TRANSACTION_STATUS_CHANGED =
            "TRANSACTION_STATUS_CHANGED";
    public static final String CONTRACT_STATUS_CHANGED =
            "CONTRACT_STATUS_CHANGED";
    public static final String COMMISSION_PAID = "COMMISSION_PAID";
    public static final String FILE_DOWNLOADED = "FILE_DOWNLOADED";
    public static final String FILE_DELETED = "FILE_DELETED";
    public static final String FILE_ACCESS_LEVEL_CHANGED =
            "FILE_ACCESS_LEVEL_CHANGED";
    public static final String PROPERTY_LEGAL_DOCUMENT_UPLOADED =
            "PROPERTY_LEGAL_DOCUMENT_UPLOADED";
    public static final String PROPERTY_LEGAL_DOCUMENT_UPDATED =
            "PROPERTY_LEGAL_DOCUMENT_UPDATED";
    public static final String PROPERTY_LEGAL_DOCUMENT_VERIFIED =
            "PROPERTY_LEGAL_DOCUMENT_VERIFIED";
    public static final String PROPERTY_LEGAL_DOCUMENT_DELETED =
            "PROPERTY_LEGAL_DOCUMENT_DELETED";
    public static final String PROPERTY_IMAGE_UPDATED =
            "PROPERTY_IMAGE_UPDATED";
    public static final String PROPERTY_IMAGES_REORDERED =
            "PROPERTY_IMAGES_REORDERED";
    public static final String USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    public static final String USER_AVATAR_UPDATED = "USER_AVATAR_UPDATED";
    public static final String USER_AVATAR_DELETED = "USER_AVATAR_DELETED";
    public static final String USER_SESSION_REVOKED = "USER_SESSION_REVOKED";
    public static final String USER_SESSIONS_REVOKED = "USER_SESSIONS_REVOKED";
    public static final String CUSTOMER_NOTE_UPDATED = "CUSTOMER_NOTE_UPDATED";
    public static final String CUSTOMER_NOTE_DELETED = "CUSTOMER_NOTE_DELETED";
    public static final String CUSTOMER_NOTE_PIN_CHANGED =
            "CUSTOMER_NOTE_PIN_CHANGED";
    public static final String CUSTOMER_REQUIREMENT_UPDATED =
            "CUSTOMER_REQUIREMENT_UPDATED";
    public static final String CUSTOMER_REQUIREMENT_DELETED =
            "CUSTOMER_REQUIREMENT_DELETED";
    public static final String CUSTOMER_TAG_CREATED = "CUSTOMER_TAG_CREATED";
    public static final String CUSTOMER_TAG_DELETED = "CUSTOMER_TAG_DELETED";
    public static final String FOLLOW_UP_TASK_UPDATED =
            "FOLLOW_UP_TASK_UPDATED";
    public static final String FOLLOW_UP_TASK_STATUS_CHANGED =
            "FOLLOW_UP_TASK_STATUS_CHANGED";
    public static final String FOLLOW_UP_TASK_CANCELLED =
            "FOLLOW_UP_TASK_CANCELLED";

    public static final String USER = "USER";
    public static final String LISTING = "LISTING";
    public static final String TRANSACTION = "TRANSACTION";
    public static final String CONTRACT = "CONTRACT";
    public static final String COMMISSION = "COMMISSION";
    public static final String FILE = "FILE";
    public static final String PROPERTY_IMAGE = "PROPERTY_IMAGE";
    public static final String PROPERTY_LEGAL_DOCUMENT = "PROPERTY_LEGAL_DOCUMENT";
    public static final String CUSTOMER_NOTE = "CUSTOMER_NOTE";
    public static final String CUSTOMER_REQUIREMENT = "CUSTOMER_REQUIREMENT";
    public static final String CUSTOMER_TAG = "CUSTOMER_TAG";
    public static final String FOLLOW_UP_TASK = "FOLLOW_UP_TASK";

    private AuditActions() {
    }
}
