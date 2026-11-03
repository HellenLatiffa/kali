package com.kali.symptoms;

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
public class SymptomEntryService {

    private final SymptomEntryRepository entries;
    private final HealthEntryValidation validation;

    public SymptomEntryService(SymptomEntryRepository entries, HealthEntryValidation validation) {
        this.entries = entries;
        this.validation = validation;
    }

    @Transactional(readOnly = true)
    public List<SymptomEntryResponse> list(KaliUserPrincipal principal, LocalDate from, LocalDate to) {
        validation.validateRange(from, to);
        return entries.findAllByUserIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
                        principal.id(), from, to
                )
                .stream()
                .map(SymptomEntryResponse::from)
                .toList();
    }

    @Transactional
    public SymptomEntryResponse create(KaliUserPrincipal principal, SymptomEntryRequest request) {
        validation.validateDate(request.entryDate());
        return SymptomEntryResponse.from(entries.save(new SymptomEntry(
                principal.id(),
                request.entryDate(),
                request.symptomType(),
                request.intensity(),
                request.notes()
        )));
    }

    @Transactional
    public SymptomEntryResponse update(KaliUserPrincipal principal, UUID id, SymptomEntryRequest request) {
        validation.validateDate(request.entryDate());
        SymptomEntry entry = entries.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entry.update(request.entryDate(), request.symptomType(), request.intensity(), request.notes());
        return SymptomEntryResponse.from(entry);
    }

    @Transactional
    public void delete(KaliUserPrincipal principal, UUID id) {
        entries.delete(entries.findByIdAndUserId(id, principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
}
