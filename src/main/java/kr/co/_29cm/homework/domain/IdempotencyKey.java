package kr.co._29cm.homework.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", 
       indexes = @Index(name = "idx_idempotency_key", columnList = "key_value"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false, unique = true, length = 255)
    private String keyValue;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    public enum Status {
        PROCESSING, COMPLETED, FAILED
    }

    public IdempotencyKey(String keyValue, Long orderId, LocalDateTime expiresAt) {
        this.keyValue = keyValue;
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.status = Status.PROCESSING;
    }

    public void markCompleted() {
        this.status = Status.COMPLETED;
    }

    public void markFailed() {
        this.status = Status.FAILED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isProcessing() {
        return this.status == Status.PROCESSING;
    }

    public boolean isCompleted() {
        return this.status == Status.COMPLETED;
    }
}
