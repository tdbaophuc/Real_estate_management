package com.javaweb.audit.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.property.entity.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "old_value_json", length = 8000)
    private String oldValueJson;

    @Column(name = "new_value_json", length = 8000)
    private String newValueJson;

    protected AuditLog() {
    }

    public AuditLog(
            User actor,
            String action,
            String resourceType,
            Long resourceId,
            String oldValueJson,
            String newValueJson
    ) {
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.oldValueJson = oldValueJson;
        this.newValueJson = newValueJson;
    }

    public Long getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getOldValueJson() {
        return oldValueJson;
    }

    public String getNewValueJson() {
        return newValueJson;
    }
}
