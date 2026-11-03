package com.kali.shared;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class HealthEntryValidation {

    private static final long MAX_RANGE_DAYS = 366;

    private final Clock clock;

    public HealthEntryValidation(Clock clock) {
        this.clock = clock;
    }

    public void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)
                || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new InvalidHealthEntryException();
        }
    }

    public void validateDate(LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now(clock))) {
            throw new InvalidHealthEntryException();
        }
    }
}
