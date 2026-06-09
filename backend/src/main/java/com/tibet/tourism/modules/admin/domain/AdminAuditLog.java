package com.tibet.tourism.modules.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs", indexes = {
        @Index(name = "idx_admin_audit_actor_created", columnList = "actor_id, created_at"),
        @Index(name = "idx_admin_audit_target_created", columnList = "target_id, created_at"),
        @Index(name = "idx_admin_audit_action_created", columnList = "action, created_at")
})
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "actor_ref", nullable = false, length = 64)
    private String actorRef;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "target_ref", nullable = false, length = 64)
    private String targetRef;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 16)
    private String result;

    @Column(nullable = false, length = 64)
    private String reason;

    @Column(name = "before_role", length = 32)
    private String beforeRole;

    @Column(name = "after_role", length = 32)
    private String afterRole;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static AdminAuditLog create(Long actorId,
                                       String actorRef,
                                       Long targetId,
                                       String targetRef,
                                       String action,
                                       String result,
                                       String reason,
                                       String beforeRole,
                                       String afterRole) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActorId(actorId);
        log.setActorRef(actorRef);
        log.setTargetId(targetId);
        log.setTargetRef(targetRef);
        log.setAction(action);
        log.setResult(result);
        log.setReason(reason);
        log.setBeforeRole(beforeRole);
        log.setAfterRole(afterRole);
        return log;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorRef() {
        return actorRef;
    }

    public void setActorRef(String actorRef) {
        this.actorRef = actorRef;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBeforeRole() {
        return beforeRole;
    }

    public void setBeforeRole(String beforeRole) {
        this.beforeRole = beforeRole;
    }

    public String getAfterRole() {
        return afterRole;
    }

    public void setAfterRole(String afterRole) {
        this.afterRole = afterRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
