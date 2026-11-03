package com.kali.menstrualcycle;

import com.kali.identity.KaliUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/periods")
public class PeriodEntryController {

    private final PeriodEntryService entries;

    public PeriodEntryController(PeriodEntryService entries) {
        this.entries = entries;
    }

    @GetMapping
    List<PeriodEntryResponse> list(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return entries.list(principal, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PeriodEntryResponse create(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @Valid @RequestBody PeriodEntryRequest request
    ) {
        return entries.create(principal, request);
    }

    @PutMapping("/{id}")
    PeriodEntryResponse update(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PeriodEntryRequest request
    ) {
        return entries.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @PathVariable UUID id
    ) {
        entries.delete(principal, id);
    }
}
