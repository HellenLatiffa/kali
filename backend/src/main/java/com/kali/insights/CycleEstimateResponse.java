package com.kali.insights;

import java.time.LocalDate;

public record CycleEstimateResponse(
        boolean estimateAvailable,
        int recordedCycleStarts,
        int intervalsUsed,
        LocalDate lastCycleStart,
        LocalDate estimatedNextPeriod,
        LocalDate earliestPossibleDate,
        LocalDate latestPossibleDate,
        Integer medianCycleDays,
        Integer shortestObservedCycleDays,
        Integer longestObservedCycleDays
) {
    static CycleEstimateResponse insufficientHistory(int recordedCycleStarts) {
        return new CycleEstimateResponse(
                false,
                recordedCycleStarts,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
