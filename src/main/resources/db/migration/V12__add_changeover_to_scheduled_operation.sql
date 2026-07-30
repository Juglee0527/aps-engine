ALTER TABLE scheduled_operation
    ADD COLUMN changeover_start_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN changeover_minutes BIGINT NOT NULL DEFAULT 0;

ALTER TABLE scheduled_operation
    ADD CONSTRAINT ck_scheduled_operation_changeover_minutes CHECK (
        changeover_minutes >= 0
    ),
    ADD CONSTRAINT ck_scheduled_operation_changeover_period CHECK (
        (changeover_minutes = 0 AND changeover_start_at IS NULL)
        OR (
            changeover_minutes > 0
            AND changeover_start_at IS NOT NULL
            AND start_at > changeover_start_at
        )
    );
