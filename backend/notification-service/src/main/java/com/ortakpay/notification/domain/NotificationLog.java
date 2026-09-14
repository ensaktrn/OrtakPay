package com.ortakpay.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A one-way audit log of notifications this service "sent" (mocked - see
 * NotificationListener, there is no real email/push provider in this phase).
 * createdAt is set explicitly at construction, not via JPA auditing: a single
 * always-populated field doesn't justify pulling in the whole auditing
 * infrastructure core-service uses for its multi-field, updatable entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
