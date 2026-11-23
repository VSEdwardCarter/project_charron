
CREATE TABLE loading_plans (
                               id UUID PRIMARY KEY,

                               movement_id UUID NOT NULL
                                   REFERENCES movements(id),

                               equipment_id UUID NOT NULL
                                   REFERENCES equipment(id),

                               cargo_space_id UUID NOT NULL
                                   REFERENCES equipment_cargo_spaces(id),

                               created_at TIMESTAMPTZ NOT NULL,

                               updated_at TIMESTAMPTZ NOT NULL,

                               CONSTRAINT chk_loading_plan_timestamps
                                   CHECK (updated_at >= created_at),

                               CONSTRAINT uq_loading_plan_movement_cargo_space
                                   UNIQUE (movement_id, cargo_space_id)
);

CREATE INDEX idx_loading_plans_movement
    ON loading_plans(movement_id);

CREATE INDEX idx_loading_plans_equipment
    ON loading_plans(equipment_id);

CREATE INDEX idx_loading_plans_cargo_space
    ON loading_plans(cargo_space_id);
