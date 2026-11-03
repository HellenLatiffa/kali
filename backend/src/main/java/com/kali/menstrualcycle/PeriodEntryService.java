package com.kali.menstrualcycle;

import com.kali.identity.KaliUserPrincipal;
import com.kali.shared.HealthEntryValidation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PeriodEntryService {

    private final PeriodEntryRepository entries;
    private final HealthEntryValidation validation;

    public PeriodEntryService(PeriodEntryRepository entries, HealthEntryValidation validation) {
        this.entries = entries;
        this.validation = validation;
    }

    @Transactional(readOnly = true)
    public List<PeriodEntryResponse> list(KaliUserPrincipal principal, LocalDate from, LocalDate to) {
        validation.validateRange(from, to);
        return entries.findAllByUserIdAndEntryDateBetweenOrderByEntryDateAsc(principal.id(), from, to)
                .stream()
                .map(PeriodEntryResponse::from)
                .toList();
    }

    @Transactional
    public PeriodEntryResponse create(KaliUserPrincipal principal, PeriodEntryRequest request) {
        validation.validateDate(request.entryDate());
        if (entries.existsByUserIdAndEntryDate(principal.id(), request.entryDate())) {
            throw new PeriodEntryConflictException();
        }
        PeriodEntry entry = entries.save(new PeriodEntry(
                principal.id(),
                request.entryDate(),
                request.bleedingType(),
                request.flowLevel(),
                request.cycleStart()
        ));
        return PeriodEntryResponse.from(entry);
    }

    @Transactional
    public PeriodEntryResponse update(KaliUserPrincipal principal, UUID id, PeriodEntryRequest request) {
        validation.validateDate(request.entryDate());
        PeriodEntry entry = findOwnedEntry(principal.id(), id);
        if (entries.existsByUserIdAndEntryDateAndIdNot(
                principal.id(),
                request.entryDate(),
                id
        )) {
            throw new PeriodEntryConflictException();
        }
        entry.update(
                request.entryDate(),
                request.bleedingType(),
                request.flowLevel(),
                request.cycleStart()
        );
        return PeriodEntryResponse.from(entry);
    }

    @Transactional
    public void delete(KaliUserPrincipal principal, UUID id) {
        entries.delete(findOwnedEntry(principal.id(), id));
    }

    private PeriodEntry findOwnedEntry(UUID userId, UUID id) {
        return entries.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
