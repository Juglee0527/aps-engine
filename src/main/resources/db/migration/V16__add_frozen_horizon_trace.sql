ALTER TABLE schedule_run
    ADD COLUMN source_schedule_run_id BIGINT,
    ADD COLUMN frozen_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE schedule_run
    ADD CONSTRAINT fk_schedule_run_source
        FOREIGN KEY (source_schedule_run_id)
        REFERENCES schedule_run (schedule_run_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT ck_schedule_run_reschedule_trace CHECK (
        (source_schedule_run_id IS NULL AND frozen_at IS NULL)
        OR (
            source_schedule_run_id IS NOT NULL
            AND frozen_at IS NOT NULL
            AND frozen_at >= planning_start
            AND source_schedule_run_id <> schedule_run_id
        )
    );

CREATE INDEX ix_schedule_run_source_id
    ON schedule_run (source_schedule_run_id);
