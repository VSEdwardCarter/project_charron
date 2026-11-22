
CREATE TABLE movement_equipment_assignments (
                                                id UUID PRIMARY KEY,

                                                movement_id UUID NOT NULL
                                                    REFERENCES movements(id),

                                                equipment_id UUID NOT NULL
                                                    REFERENCES equipment(id),

                                                role VARCHAR(30) NOT NULL,

                                                assigned_at TIMESTAMPTZ NOT NULL,

                                                CONSTRAINT chk_movement_equipment_role
                                                    CHECK (
                                                        role IN (
                                                                 'POWER_UNIT',
                                                                 'CARGO_CARRIER',
                                                                 'SUPPORT'
                                                            )
                                                        ),

                                                CONSTRAINT uq_movement_equipment_role
                                                    UNIQUE (movement_id, equipment_id, role)
);

CREATE INDEX idx_movement_equipment_movement
    ON movement_equipment_assignments (movement_id);

CREATE INDEX idx_movement_equipment_equipment
    ON movement_equipment_assignments (equipment_id);
