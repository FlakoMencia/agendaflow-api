package com.flakomencia.agendaflow.identity.domain;

import java.time.OffsetDateTime;

import org.hibernate.Hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users", schema = "agendaflow")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "second_last_name", length = 80)
    private String secondLastName;

    @Column(length = 30)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at", insertable = false)
    private OffsetDateTime deletedAt;

    protected AppUser() {
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getSecondLastName() { return secondLastName; }
    public String getPhone() { return phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isActive() { return active; }
    public boolean isLocked() { return locked; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public OffsetDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }

    public void recordFailedLogin(int maximumFailedAttempts) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximumFailedAttempts) {
            locked = true;
        }
    }

    public void recordSuccessfulLogin(OffsetDateTime loggedInAt) {
        failedLoginAttempts = 0;
        lastLoginAt = loggedInAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        AppUser that = (AppUser) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
