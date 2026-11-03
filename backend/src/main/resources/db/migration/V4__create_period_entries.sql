CREATE TABLE period_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    bleeding_type VARCHAR(20) NOT NULL,
    flow_level VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT period_entries_bleeding_type_check
        CHECK (bleeding_type IN ('PERIOD', 'SPOTTING')),
    CONSTRAINT period_entries_flow_level_check
        CHECK (flow_level IN ('LIGHT', 'MEDIUM', 'HEAVY')),
    CONSTRAINT period_entries_user_date_unique UNIQUE (user_id, entry_date)
);
