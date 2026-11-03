package com.kali.wellbeing;

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
@Table(name = "wellbeing_entries")
public class WellbeingEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MoodLevel mood;

    @Column(name = "stress_level")
    private Integer stressLevel;

    @Column(name = "energy_level")
    private Integer energyLevel;

    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    @Column(columnDefinition = "TEXT")
    private String reflection;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WellbeingEntry() {
    }

    public WellbeingEntry(
            UUID userId,
            LocalDate entryDate,
            MoodLevel mood,
            Integer stressLevel,
            Integer energyLevel,
            Integer sleepQuality,
            String reflection
    ) {
        id = UUID.randomUUID();
        this.userId = userId;
        update(entryDate, mood, stressLevel, energyLevel, sleepQuality, reflection);
    }

    public void update(
            LocalDate entryDate,
            MoodLevel mood,
            Integer stressLevel,
            Integer energyLevel,
            Integer sleepQuality,
            String reflection
    ) {
        this.entryDate = entryDate;
        this.mood = mood;
        this.stressLevel = stressLevel;
        this.energyLevel = energyLevel;
        this.sleepQuality = sleepQuality;
        this.reflection = reflection;
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

    public MoodLevel getMood() {
        return mood;
    }

    public Integer getStressLevel() {
        return stressLevel;
    }

    public Integer getEnergyLevel() {
        return energyLevel;
    }

    public Integer getSleepQuality() {
        return sleepQuality;
    }

    public String getReflection() {
        return reflection;
    }
}
