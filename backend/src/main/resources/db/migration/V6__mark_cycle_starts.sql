ALTER TABLE period_entries
    ADD COLUMN cycle_start BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE period_entries
    ADD CONSTRAINT period_entries_cycle_start_type_check
        CHECK (NOT cycle_start OR bleeding_type = 'PERIOD');

CREATE INDEX period_entries_cycle_start_idx
    ON period_entries (user_id, cycle_start, entry_date);
