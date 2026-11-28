package ec.edu.uce.vigia.identityconfig.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A person who has signed in at least once (US-021). The identity lives in Keycloak (ADR-050); S-09 keeps
 * the subject, the last known name and roles, and when the user was first and last seen.
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "subject", nullable = false, updatable = false)
    private String subject;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "roles", nullable = false)
    private String roles;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected UserProfile() {}

    public UserProfile(String subject, Instant now) {
        this.subject = subject;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
        this.username = subject;
        this.roles = "";
    }

    /** Copies the claims of the latest token. */
    public void refresh(String username, String email, String displayName, String roles, Instant now) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.roles = roles;
        this.lastSeenAt = now;
    }

    public String getSubject() {
        return subject;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoles() {
        return roles;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
