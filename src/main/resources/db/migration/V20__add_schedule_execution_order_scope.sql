CREATE TABLE schedule_execution_order_scope (
    schedule_execution_id BIGINT NOT NULL,
    production_order_id BIGINT NOT NULL,
    CONSTRAINT pk_schedule_execution_order_scope PRIMARY KEY (
        schedule_execution_id,
        production_order_id
    ),
    CONSTRAINT fk_schedule_execution_order_scope_execution
        FOREIGN KEY (schedule_execution_id)
        REFERENCES schedule_execution (schedule_execution_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_schedule_execution_order_scope_order
        FOREIGN KEY (production_order_id)
        REFERENCES production_order (production_order_id)
        ON DELETE RESTRICT
);

CREATE INDEX ix_schedule_execution_order_scope_order
    ON schedule_execution_order_scope (production_order_id);
