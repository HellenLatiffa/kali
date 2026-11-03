package com.kali.symptoms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SymptomEntryRepository extends JpaRepository<SymptomEntry, UUID> {

    List<SymptomEntry> findAllByUserIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
            UUID userId,
            LocalDate from,
            LocalDate to
    );

    List<SymptomEntry> findAllByUserIdOrderByEntryDateAscCreatedAtAsc(UUID userId);

    java.util.Optional<SymptomEntry> findByIdAndUserId(UUID id, UUID userId);
}
