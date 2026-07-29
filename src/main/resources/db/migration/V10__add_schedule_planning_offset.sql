ALTER TABLE schedule_run
    ADD COLUMN planning_offset_seconds INTEGER NOT NULL DEFAULT 0;

ALTER TABLE schedule_run
    ADD CONSTRAINT ck_schedule_run_planning_offset CHECK (
        planning_offset_seconds BETWEEN -64800 AND 64800
    );
