package com.kali.menstrualcycle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodEntryRepository extends JpaRepository<PeriodEntry, UUID> {

    List<PeriodEntry> findAllByUserIdOrderByEntryDateAsc(UUID userId);

    List<PeriodEntry> findTop7ByUserIdAndCycleStartTrueAndBleedingTypeOrderByEntryDateDesc(
            UUID userId,
            BleedingType bleedingType
    );

    long countByUserIdAndCycleStartTrueAndBleedingType(UUID userId, BleedingType bleedingType);

    List<PeriodEntry> findAllByUserIdAndEntryDateBetweenOrderByEntryDateAsc(
            UUID userId,
            LocalDate from,
            LocalDate to
    );

    Optional<PeriodEntry> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndEntryDate(UUID userId, LocalDate entryDate);

    boolean existsByUserIdAndEntryDateAndIdNot(UUID userId, LocalDate entryDate, UUID id);
}
