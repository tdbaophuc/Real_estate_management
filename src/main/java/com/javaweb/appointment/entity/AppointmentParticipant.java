package com.javaweb.appointment.entity;

import com.javaweb.appointment.enums.AppointmentParticipantRole;
import com.javaweb.appointment.enums.ParticipantResponseStatus;
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

import java.time.Instant;

@Entity
@Table(
        name = "appointment_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_appointment_participants_appointment_user",
                columnNames = {"appointment_id", "user_id"}
        )
)
public class AppointmentParticipant extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 30)
    private AppointmentParticipantRole participantRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 30)
    private ParticipantResponseStatus responseStatus = ParticipantResponseStatus.INVITED;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(length = 1000)
    private String notes;

    protected AppointmentParticipant() {
    }

    public AppointmentParticipant(User user, AppointmentParticipantRole participantRole) {
        this.user = user;
        this.participantRole = participantRole;
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

    public User getUser() {
        return user;
    }

    public AppointmentParticipantRole getParticipantRole() {
        return participantRole;
    }

    public void setParticipantRole(AppointmentParticipantRole participantRole) {
        this.participantRole = participantRole;
    }

    public ParticipantResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(ParticipantResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
