package com.javaweb.appointment.entity;

import com.javaweb.appointment.enums.ViewingInterestLevel;
import com.javaweb.auth.entity.User;
import com.javaweb.property.entity.AuditableEntity;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "viewing_feedbacks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_viewing_feedbacks_appointment_submitter",
                columnNames = {"appointment_id", "submitted_by"}
        )
)
public class ViewingFeedback extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by", nullable = false, updatable = false)
    private User submittedBy;

    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_level", nullable = false, length = 30)
    private ViewingInterestLevel interestLevel;

    @Column(length = 4000)
    private String comments;

    @Column(name = "positive_points", length = 2000)
    private String positivePoints;

    @Column(length = 2000)
    private String concerns;

    @Column(name = "next_action", length = 1000)
    private String nextAction;

    protected ViewingFeedback() {
    }

    public ViewingFeedback(User submittedBy, ViewingInterestLevel interestLevel) {
        this.submittedBy = submittedBy;
        this.interestLevel = interestLevel;
    }

    void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public ViewingInterestLevel getInterestLevel() {
        return interestLevel;
    }

    public void setInterestLevel(ViewingInterestLevel interestLevel) {
        this.interestLevel = interestLevel;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPositivePoints() {
        return positivePoints;
    }

    public void setPositivePoints(String positivePoints) {
        this.positivePoints = positivePoints;
    }

    public String getConcerns() {
        return concerns;
    }

    public void setConcerns(String concerns) {
        this.concerns = concerns;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }
}
