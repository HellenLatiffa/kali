package com.kali.wellbeing;

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
@RequestMapping("/api/v1/mood-entries")
public class WellbeingEntryController {

    private final WellbeingEntryService entries;

    public WellbeingEntryController(WellbeingEntryService entries) {
        this.entries = entries;
    }

    @GetMapping
    List<WellbeingEntryResponse> list(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return entries.list(principal, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WellbeingEntryResponse create(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @Valid @RequestBody WellbeingEntryRequest request
    ) {
        return entries.create(principal, request);
    }

    @PutMapping("/{id}")
    WellbeingEntryResponse update(
            @AuthenticationPrincipal KaliUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody WellbeingEntryRequest request
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
