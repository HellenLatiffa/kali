package com.kali.wellbeing;

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
public class WellbeingEntryService {

    private final WellbeingEntryRepository entries;
    private final HealthEntryValidation validation;

    public WellbeingEntryService(WellbeingEntryRepository entries, HealthEntryValidation validation) {
        this.entries = entries;
        this.validation = validation;
    }

    @Transactional(readOnly = true)
    public List<WellbeingEntryResponse> list(KaliUserPrincipal principal, LocalDate from, LocalDate to) {
        validation.validateRange(from, to);
        return entries.findAllByUserIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
                        principal.id(), from, to
                )
                .stream()
                .map(WellbeingEntryResponse::from)
                .toList();
    }

    @Transactional
    public WellbeingEntryResponse create(KaliUserPrincipal principal, WellbeingEntryRequest request) {
        validation.validateDate(request.entryDate());
        return WellbeingEntryResponse.from(entries.save(new WellbeingEntry(
                principal.id(),
                request.entryDate(),
                request.mood(),
                request.stressLevel(),
                request.energyLevel(),
                request.sleepQuality(),
                request.reflection()
        )));
    }

    @Transactional
    public WellbeingEntryResponse update(KaliUserPrincipal principal, UUID id, WellbeingEntryRequest request) {
        validation.validateDate(request.entryDate());
        WellbeingEntry entry = entries.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entry.update(
                request.entryDate(),
                request.mood(),
                request.stressLevel(),
                request.energyLevel(),
                request.sleepQuality(),
                request.reflection()
        );
        return WellbeingEntryResponse.from(entry);
    }

    @Transactional
    public void delete(KaliUserPrincipal principal, UUID id) {
        entries.delete(entries.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
}
