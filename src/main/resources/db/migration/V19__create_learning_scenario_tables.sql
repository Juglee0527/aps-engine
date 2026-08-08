CREATE TABLE learning_scenario_instance (
    id BIGSERIAL PRIMARY KEY,
    request_key UUID NOT NULL,
    scenario_key VARCHAR(50) NOT NULL,
    namespace VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    planning_start TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_learning_scenario_request_key UNIQUE (request_key),
    CONSTRAINT uk_learning_scenario_namespace UNIQUE (namespace),
    CONSTRAINT ck_learning_scenario_status CHECK (
        status IN ('READY', 'RESET')
    )
);

CREATE TABLE learning_scenario_entity (
    id BIGSERIAL PRIMARY KEY,
    scenario_instance_id BIGINT NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    CONSTRAINT fk_learning_scenario_entity_instance
        FOREIGN KEY (scenario_instance_id)
        REFERENCES learning_scenario_instance (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_learning_scenario_entity UNIQUE (
        scenario_instance_id,
        entity_type,
        entity_id
    )
);

CREATE INDEX idx_learning_scenario_entity_instance
    ON learning_scenario_entity (scenario_instance_id);
