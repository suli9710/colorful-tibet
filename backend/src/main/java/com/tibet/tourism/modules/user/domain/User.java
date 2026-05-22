package com.tibet.tourism.modules.user.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tibet.tourism.common.security.PiiCryptoConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password; // BCrypt哈希，用于登录验证

    @Column(unique = true)
    private String nickname;
    private String avatar;
    @Convert(converter = PiiCryptoConverter.class)
    @Column(length = 512)
    private String phone;
    private String city; // 用户所在城市
    @Column(length = 64)
    private String ipAddress; // 最后登录IP地址哈希
    private LocalDateTime lastLoginAt; // 最后登录时间
    private Boolean mustChangePassword = false;

    @Column(length = 64)
    private String allowedLoginFingerprintHash;

    @Column(name = "session_version", nullable = false)
    private Long sessionVersion = 0L;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getAllowedLoginFingerprintHash() {
        return allowedLoginFingerprintHash;
    }

    public void setAllowedLoginFingerprintHash(String allowedLoginFingerprintHash) {
        this.allowedLoginFingerprintHash = allowedLoginFingerprintHash;
    }

    public Long getSessionVersion() {
        return sessionVersion == null ? 0L : sessionVersion;
    }

    public void setSessionVersion(Long sessionVersion) {
        this.sessionVersion = sessionVersion == null ? 0L : sessionVersion;
    }

    public void incrementSessionVersion() {
        setSessionVersion(getSessionVersion() + 1);
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public enum Role {
        ADMIN, USER
    }
}
