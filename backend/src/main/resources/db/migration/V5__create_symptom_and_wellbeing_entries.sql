CREATE TABLE symptom_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    symptom_type VARCHAR(40) NOT NULL,
    intensity INTEGER NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT symptom_entries_type_check CHECK (
        symptom_type IN (
            'CRAMPS',
            'PAIN',
            'HEADACHE',
            'BLOATING',
            'ACNE',
            'BREAST_TENDERNESS',
            'BOWEL_CHANGES',
            'ENERGY_CHANGE',
            'APPETITE_CHANGE',
            'SLEEP_CHANGE',
            'DISCHARGE'
        )
    ),
    CONSTRAINT symptom_entries_intensity_check CHECK (intensity BETWEEN 1 AND 5),
    CONSTRAINT symptom_entries_notes_length_check
        CHECK (notes IS NULL OR char_length(notes) <= 2000)
);

CREATE INDEX symptom_entries_user_date_idx
    ON symptom_entries (user_id, entry_date, created_at);

CREATE TABLE wellbeing_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entry_date DATE NOT NULL,
    mood VARCHAR(20) NOT NULL,
    stress_level INTEGER,
    energy_level INTEGER,
    sleep_quality INTEGER,
    reflection TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT wellbeing_entries_mood_check
        CHECK (mood IN ('VERY_LOW', 'LOW', 'NEUTRAL', 'GOOD', 'VERY_GOOD')),
    CONSTRAINT wellbeing_entries_stress_check
        CHECK (stress_level IS NULL OR stress_level BETWEEN 1 AND 5),
    CONSTRAINT wellbeing_entries_energy_check
        CHECK (energy_level IS NULL OR energy_level BETWEEN 1 AND 5),
    CONSTRAINT wellbeing_entries_sleep_quality_check
        CHECK (sleep_quality IS NULL OR sleep_quality BETWEEN 1 AND 5),
    CONSTRAINT wellbeing_entries_reflection_length_check
        CHECK (reflection IS NULL OR char_length(reflection) <= 2000)
);

CREATE INDEX wellbeing_entries_user_date_idx
    ON wellbeing_entries (user_id, entry_date, created_at);
