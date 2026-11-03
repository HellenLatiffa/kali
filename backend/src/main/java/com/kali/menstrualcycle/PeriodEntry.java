package com.kali.menstrualcycle;

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
@Table(name = "period_entries")
public class PeriodEntry {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "bleeding_type", nullable = false, length = 20)
    private BleedingType bleedingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_level", nullable = false, length = 20)
    private FlowLevel flowLevel;

    @Column(name = "cycle_start", nullable = false)
    private boolean cycleStart;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PeriodEntry() {
    }

    public PeriodEntry(
            UUID userId,
            LocalDate entryDate,
            BleedingType bleedingType,
            FlowLevel flowLevel,
            boolean cycleStart
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.update(entryDate, bleedingType, flowLevel, cycleStart);
    }

    public void update(
            LocalDate entryDate,
            BleedingType bleedingType,
            FlowLevel flowLevel,
            boolean cycleStart
    ) {
        this.entryDate = entryDate;
        this.bleedingType = bleedingType;
        this.flowLevel = flowLevel;
        this.cycleStart = cycleStart;
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

    public BleedingType getBleedingType() {
        return bleedingType;
    }

    public FlowLevel getFlowLevel() {
        return flowLevel;
    }

    public boolean isCycleStart() {
        return cycleStart;
    }
}
