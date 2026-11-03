package com.chubb.apac.claims.domain;

import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class ClaimNote {
    private String author;
    private String authorRole; // CLAIMANT or STAFF
    private String message;
    private Instant createdAt;

    public ClaimNote() {}

    public ClaimNote(String author, String authorRole, String message, Instant createdAt) {
        this.author = author;
        this.authorRole = authorRole;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
