package com.kali.symptoms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "symptom_entries")
public class SymptomEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "symptom_type", nullable = false, length = 40)
    private SymptomType symptomType;

    @Column(nullable = false)
    private int intensity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SymptomEntry() {
    }

    public SymptomEntry(
            UUID userId,
            LocalDate entryDate,
            SymptomType symptomType,
            int intensity,
            String notes
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.update(entryDate, symptomType, intensity, notes);
    }

    public void update(LocalDate entryDate, SymptomType symptomType, int intensity, String notes) {
        this.entryDate = entryDate;
        this.symptomType = symptomType;
        this.intensity = intensity;
        this.notes = notes;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public SymptomType getSymptomType() {
        return symptomType;
    }

    public int getIntensity() {
        return intensity;
    }

    public String getNotes() {
        return notes;
    }
}
