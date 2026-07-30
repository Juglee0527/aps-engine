ALTER TABLE schedule_run
    ADD COLUMN dispatching_rule VARCHAR(30) NOT NULL
        DEFAULT 'EXPLICIT_PRIORITY',
    ADD COLUMN total_tardiness_minutes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN delayed_order_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN makespan_minutes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN machine_utilization_percent NUMERIC(7, 2)
        NOT NULL DEFAULT 0;

UPDATE schedule_run schedule
SET makespan_minutes = FLOOR(
        EXTRACT(EPOCH FROM (
            schedule.scheduling_end - schedule.planning_start
        )) / 60
    )::BIGINT,
    delayed_order_count = (
        SELECT COUNT(DISTINCT operation.production_order_id)
        FROM scheduled_operation operation
        WHERE operation.schedule_run_id = schedule.schedule_run_id
          AND operation.delayed = TRUE
    ),
    total_tardiness_minutes = COALESCE((
        SELECT SUM(GREATEST(
            0,
            FLOOR(EXTRACT(EPOCH FROM (
                completion.completion_at - production_order.due_at
            )) / 60)
        ))::BIGINT
        FROM (
            SELECT
                operation.production_order_id,
                MAX(operation.end_at) AS completion_at
            FROM scheduled_operation operation
            WHERE operation.schedule_run_id = schedule.schedule_run_id
            GROUP BY operation.production_order_id
        ) completion
        JOIN production_order
          ON production_order.production_order_id =
             completion.production_order_id
    ), 0);

ALTER TABLE schedule_run
    ADD CONSTRAINT ck_schedule_run_dispatching_rule CHECK (
        dispatching_rule IN ('EXPLICIT_PRIORITY', 'EDD', 'SPT')
    ),
    ADD CONSTRAINT ck_schedule_run_total_tardiness CHECK (
        total_tardiness_minutes >= 0
    ),
    ADD CONSTRAINT ck_schedule_run_delayed_order_count CHECK (
        delayed_order_count >= 0
    ),
    ADD CONSTRAINT ck_schedule_run_makespan CHECK (
        makespan_minutes >= 0
    ),
    ADD CONSTRAINT ck_schedule_run_machine_utilization CHECK (
        machine_utilization_percent BETWEEN 0 AND 100
    );
