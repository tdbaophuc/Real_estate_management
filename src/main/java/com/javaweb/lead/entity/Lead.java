package com.javaweb.lead.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.customer.entity.Customer;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.enums.LeadPriority;
import com.javaweb.listing.entity.Listing;
import com.javaweb.property.entity.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leads")
public class Lead extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private LeadSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private User currentAssignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadPipelineStatus status = LeadPipelineStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadPriority priority = LeadPriority.MEDIUM;

    private Integer score;

    @Column(length = 4000)
    private String message;

    @Column(name = "lost_reason", length = 1000)
    private String lostReason;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeadAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeadNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeadActivity> activities = new ArrayList<>();

    @OneToMany(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowUpTask> followUpTasks = new ArrayList<>();

    protected Lead() {
    }

    public Lead(String code, LeadSource source, String fullName) {
        this.code = code;
        this.source = source;
        this.fullName = fullName;
    }

    public LeadAssignment addAssignment(LeadAssignment assignment) {
        assignments.add(assignment);
        assignment.setLead(this);
        return assignment;
    }

    public LeadNote addNote(LeadNote note) {
        notes.add(note);
        note.setLead(this);
        return note;
    }

    public LeadActivity addActivity(LeadActivity activity) {
        activities.add(activity);
        activity.setLead(this);
        return activity;
    }

    public FollowUpTask addFollowUpTask(FollowUpTask task) {
        followUpTasks.add(task);
        task.setLead(this);
        return task;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LeadSource getSource() {
        return source;
    }

    public void setSource(LeadSource source) {
        this.source = source;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public User getCurrentAssignee() {
        return currentAssignee;
    }

    public void setCurrentAssignee(User currentAssignee) {
        this.currentAssignee = currentAssignee;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LeadPipelineStatus getStatus() {
        return status;
    }

    public void setStatus(LeadPipelineStatus status) {
        this.status = status;
    }

    public LeadPriority getPriority() {
        return priority;
    }

    public void setPriority(LeadPriority priority) {
        this.priority = priority;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLostReason() {
        return lostReason;
    }

    public void setLostReason(String lostReason) {
        this.lostReason = lostReason;
    }

    public Instant getLastContactedAt() {
        return lastContactedAt;
    }

    public void setLastContactedAt(Instant lastContactedAt) {
        this.lastContactedAt = lastContactedAt;
    }

    public Instant getConvertedAt() {
        return convertedAt;
    }

    public void setConvertedAt(Instant convertedAt) {
        this.convertedAt = convertedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<LeadAssignment> getAssignments() {
        return assignments;
    }

    public List<LeadNote> getNotes() {
        return notes;
    }

    public List<LeadActivity> getActivities() {
        return activities;
    }

    public List<FollowUpTask> getFollowUpTasks() {
        return followUpTasks;
    }
}
