
CREATE TABLE loading_plan_cargo (
                                    id UUID PRIMARY KEY,

                                    loading_plan_id UUID NOT NULL
                                        REFERENCES loading_plans(id)
                                            ON DELETE CASCADE,

                                    cargo_id UUID NOT NULL
                                        REFERENCES cargo(id),

                                    quantity INTEGER NOT NULL,

                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                    CONSTRAINT chk_loading_plan_cargo_quantity
                                        CHECK (quantity > 0),

                                    CONSTRAINT chk_loading_plan_cargo_timestamps
                                        CHECK (updated_at >= created_at),

                                    CONSTRAINT uq_loading_plan_cargo
                                        UNIQUE (loading_plan_id, cargo_id)
);

CREATE INDEX idx_loading_plan_cargo_plan
    ON loading_plan_cargo(loading_plan_id);

CREATE INDEX idx_loading_plan_cargo_cargo
    ON loading_plan_cargo(cargo_id);
