package com.kali.wellbeing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WellbeingEntryRepository extends JpaRepository<WellbeingEntry, UUID> {

    List<WellbeingEntry> findAllByUserIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
            UUID userId,
            LocalDate from,
            LocalDate to
    );

    List<WellbeingEntry> findAllByUserIdOrderByEntryDateAscCreatedAtAsc(UUID userId);

    Optional<WellbeingEntry> findByIdAndUserId(UUID id, UUID userId);
}
