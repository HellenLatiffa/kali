package com.kali.insights;

import com.kali.identity.KaliUserPrincipal;
import com.kali.menstrualcycle.BleedingType;
import com.kali.menstrualcycle.PeriodEntry;
import com.kali.menstrualcycle.PeriodEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CycleEstimateService {

    private static final int MINIMUM_CYCLE_STARTS = 4;
    private static final int MAXIMUM_INTERVALS_USED = 6;

    private final PeriodEntryRepository periodEntries;

    public CycleEstimateService(PeriodEntryRepository periodEntries) {
        this.periodEntries = periodEntries;
    }

    @Transactional(readOnly = true)
    public CycleEstimateResponse estimate(KaliUserPrincipal principal) {
        long recordedCycleStarts = periodEntries.countByUserIdAndCycleStartTrueAndBleedingType(
                principal.id(),
                BleedingType.PERIOD
        );
        if (recordedCycleStarts < MINIMUM_CYCLE_STARTS) {
            return CycleEstimateResponse.insufficientHistory(Math.toIntExact(recordedCycleStarts));
        }

        List<PeriodEntry> starts = new ArrayList<>(
                periodEntries.findTop7ByUserIdAndCycleStartTrueAndBleedingTypeOrderByEntryDateDesc(
                        principal.id(),
                        BleedingType.PERIOD
                )
        );
        if (starts.size() < MINIMUM_CYCLE_STARTS) {
            return CycleEstimateResponse.insufficientHistory(starts.size());
        }
        Collections.reverse(starts);
        List<Integer> intervals = new ArrayList<>();
        for (int index = 1; index < starts.size(); index++) {
            intervals.add(Math.toIntExact(ChronoUnit.DAYS.between(
                    starts.get(index - 1).getEntryDate(),
                    starts.get(index).getEntryDate()
            )));
        }
        if (intervals.size() > MAXIMUM_INTERVALS_USED) {
            intervals = new ArrayList<>(intervals.subList(
                    intervals.size() - MAXIMUM_INTERVALS_USED,
                    intervals.size()
            ));
        }

        List<Integer> sortedIntervals = new ArrayList<>(intervals);
        Collections.sort(sortedIntervals);
        int shortest = sortedIntervals.get(0);
        int longest = sortedIntervals.get(sortedIntervals.size() - 1);
        int median = median(sortedIntervals);
        LocalDate lastStart = starts.get(starts.size() - 1).getEntryDate();

        return new CycleEstimateResponse(
                true,
                Math.toIntExact(recordedCycleStarts),
                intervals.size(),
                lastStart,
                lastStart.plusDays(median),
                lastStart.plusDays(shortest),
                lastStart.plusDays(longest),
                median,
                shortest,
                longest
        );
    }

    private static int median(List<Integer> sortedValues) {
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return sortedValues.get(middle);
        }
        return (int) Math.round((sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2.0);
    }
}
